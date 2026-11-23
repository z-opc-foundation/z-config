// Run with: node --test test/service-list.test.mjs (installed dependencies only).
import test from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import vm from 'node:vm'
import {fileURLToPath} from 'node:url'
import {createServer, transformWithEsbuild} from 'vite'
import axios from 'axios'

const root = fileURLToPath(new URL('../', import.meta.url))

test('real request client: single /api prefix, query contract, bean normalization and map keys', async () => {
    const server = await createServer({root, logLevel: 'error', server: {middlewareMode: true, hmr: false}})
    const storageDescriptor = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
    Object.defineProperty(globalThis, 'localStorage', {configurable: true, value: {getItem: () => null}})
    try {
        const {default: request} = await server.ssrLoadModule('/src/services/request.js')
        const {namingApi} = await server.ssrLoadModule('/src/services/api.js')
        const urls = []
        let response = []
        request.defaults.adapter = async config => {
            urls.push(axios.getUri(config))
            return {data: {code: 200, data: response}, status: 200, statusText: 'OK', headers: {}, config}
        }
        const service = {serviceName: 'orders', group: 'blue', namespace: 'public', clusters: {}, healthyInstanceCount: 1, totalInstanceCount: 1, status: '健康'}
        response = [service]
        assert.deepEqual(await namingApi.listServices(), [service])
        assert.equal(urls.pop(), '/api/naming/listServices')

        const common = {group: 'blue', namespace: 'public', ip: '127.0.0.2', port: 8080, healthy: false, enabled: true, ephemeral: false, weight: 0, metadata: {cluster_name: 'literal map key', payload: '{not JSON'}}
        for (const snake of [false, true]) {
            response = [{...common, ...(snake ? {service_name: 'orders', instance_id: 'id-1', cluster_name: 'east'} : {serviceName: 'orders', instanceId: 'id-1', clusterName: 'east'})}]
            const [instance] = await namingApi.getInstances('orders / &', 'blue / &', 'public / &')
            const url = new URL(urls.pop(), 'http://isolated.test')
            assert.equal(url.pathname, '/api/naming/getAllInstances')
            assert.deepEqual(Object.fromEntries(url.searchParams), {serviceName: 'orders / &', group: 'blue / &', namespace: 'public / &'})
            assert.equal(instance.serviceName, 'orders')
            assert.equal(instance.instanceId, 'id-1')
            assert.equal(instance.clusterName, 'east')
            for (const [key, value] of Object.entries(common)) assert.deepEqual(instance[key], value)
        }
        await namingApi.getInstances('orders', 'blue')
        assert.deepEqual(Object.fromEntries(new URL(urls.pop(), 'http://isolated.test').searchParams), {serviceName: 'orders', group: 'blue'})
        await namingApi.getInstances('orders')
        assert.deepEqual(Object.fromEntries(new URL(urls.pop(), 'http://isolated.test').searchParams), {serviceName: 'orders'})
        await namingApi.getInstances('orders', 'blue', '')
        assert.equal(new URL(urls.pop(), 'http://isolated.test').searchParams.get('namespace'), '')
        response = {unexpected: []}
        await assert.rejects(namingApi.getInstances('orders'), /响应不是数组/)
    } finally {
        if (storageDescriptor) Object.defineProperty(globalThis, 'localStorage', storageDescriptor)
        else delete globalThis.localStorage
        await server.close()
    }
})

// Exercise the actual page handlers without an additional DOM dependency.
const {code} = await transformWithEsbuild(await readFile(`${root}src/pages/service/ServiceList.jsx`, 'utf8'), 'ServiceList.jsx', {
    loader: 'jsx', format: 'cjs', jsxFactory: 'h', jsxFragment: 'Fragment',
})
function harness() {
    const slots = [], effects = [], requests = {list: [], instances: []}
    let cursor = 0, writes = 0
    const h = (type, props, ...children) => ({type, props: {...props, ...(children.length ? {children} : {})}})
    const enqueue = (kind, args) => new Promise((resolve, reject) => requests[kind].push({args, resolve, reject}))
    const react = {
        useState(initial) {
            const index = cursor++
            if (!(index in slots)) slots[index] = initial
            return [slots[index], value => {writes++; slots[index] = typeof value === 'function' ? value(slots[index]) : value}]
        },
        useRef(initial) {
            const index = cursor++
            if (!(index in slots)) slots[index] = {current: initial}
            return slots[index]
        },
        useEffect(effect) {
            const index = cursor++
            if (!(index in slots)) {slots[index] = true; effects.push(effect)}
        },
    }
    const components = new Proxy({}, {get: (_, key) => key})
    const module = {exports: {}}
    vm.runInNewContext(code, {module, exports: module.exports, h, Fragment: 'Fragment', require: name => {
        if (name === 'react') return react
        if (name === '../../services/api') return {namingApi: {listServices: (...args) => enqueue('list', args), getInstances: (...args) => enqueue('instances', args)}}
        return components
    }})
    const render = () => {cursor = 0; return module.exports.default()}
    render()
    const cleanups = effects.map(effect => effect())
    return {render, requests, unmount: () => cleanups.forEach(cleanup => cleanup()), get writes() {return writes}}
}
function find(node, type) {
    if (!node) return undefined
    if (Array.isArray(node)) return node.map(child => find(child, type)).find(Boolean)
    if (node.type === type) return node
    return find(node.props?.children, type)
}
const flush = () => new Promise(resolve => setImmediate(resolve))
const list = h => find(h.render(), 'ListTable').props
const drawer = h => find(h.render(), 'Drawer').props
const select = (h, service) => list(h).columns.find(column => column.key === 'action').render(null, service).props.onClick()
const serviceA = {serviceName: 'orders', group: 'blue', namespace: 'public'}
const serviceB = {serviceName: 'orders', group: 'green', namespace: 'private'}

test('list latest-wins, stale failure, retry, invalid response and unmount guards', async () => {
    const h = harness()
    list(h).cardExtra.props.onClick()
    h.requests.list[1].resolve([serviceA])
    await flush()
    h.requests.list[0].reject(Error('stale failure'))
    await flush()
    assert.deepEqual(list(h).rows, [serviceA])
    assert.equal(find(h.render(), 'ErrorState'), undefined)
    assert.equal(list(h).loading, false)
    list(h).cardExtra.props.onClick()
    h.requests.list[2].resolve({not: 'an array'})
    await flush()
    assert.match(find(h.render(), 'ErrorState').props.error.message, /响应不是数组/)
    find(h.render(), 'ErrorState').props.onRetry()
    h.requests.list[3].resolve([])
    await flush()
    assert.equal(find(h.render(), 'ErrorState'), undefined)
    assert.equal(list(h).rows.length, 0)
    list(h).cardExtra.props.onClick()
    h.unmount()
    const writes = h.writes
    h.requests.list[4].resolve([serviceB])
    await flush()
    assert.equal(h.writes, writes)
})

test('drawer selection, close, retry, empty, refresh ordering and unmount guards', async () => {
    const h = harness()
    h.requests.list[0].resolve([serviceA, serviceB])
    await flush()
    select(h, serviceA)
    assert.equal(drawer(h).open, true)
    assert.equal(find(h.render(), 'Table'), undefined)
    assert.ok(find(h.render(), 'Spin'), 'loading is distinct from an empty instance table')
    drawer(h).onClose()
    select(h, serviceB)
    assert.deepEqual(h.requests.instances[1].args, ['orders', 'green', 'private'])
    h.requests.instances[1].resolve([{instanceId: 'green-id'}])
    await flush()
    h.requests.instances[0].resolve([{instanceId: 'stale-blue'}])
    await flush()
    assert.equal(find(h.render(), 'Table').props.dataSource[0].instanceId, 'green-id')
    drawer(h).extra.props.onClick()
    h.requests.instances[2].reject(Error('instance failure'))
    await flush()
    assert.equal(find(h.render(), 'Table'), undefined)
    assert.equal(find(h.render(), 'ErrorState').props.error.message, 'instance failure')
    find(h.render(), 'ErrorState').props.onRetry()
    h.requests.instances[3].resolve([])
    await flush()
    assert.equal(find(h.render(), 'ErrorState'), undefined)
    assert.equal(find(h.render(), 'EmptyState').props.title, '暂无实例')
    drawer(h).extra.props.onClick()
    drawer(h).extra.props.onClick()
    h.requests.instances[5].resolve([{instanceId: 'newest'}])
    await flush()
    h.requests.instances[4].reject(Error('stale refresh'))
    await flush()
    assert.equal(find(h.render(), 'ErrorState'), undefined)
    assert.equal(find(h.render(), 'Table').props.dataSource[0].instanceId, 'newest')
    drawer(h).extra.props.onClick()
    drawer(h).onClose()
    h.requests.instances[6].reject(Error('closed failure'))
    await flush()
    assert.equal(drawer(h).open, false)
    assert.equal(find(h.render(), 'ErrorState'), undefined)
    select(h, serviceA)
    h.unmount()
    const writes = h.writes
    h.requests.instances[7].resolve([{instanceId: 'after unmount'}])
    await flush()
    assert.equal(h.writes, writes)
})

test('no pinned columns, internal scrolling, full mobile drawer and safe metadata rendering', async () => {
    const h = harness()
    h.requests.list[0].resolve([serviceA])
    await flush()
    assert.equal(list(h).columns.some(column => column.fixed), false)
    assert.ok(list(h).tableProps.scroll.x)
    select(h, serviceA)
    h.requests.instances[0].resolve([{instanceId: 'id'}])
    await flush()
    assert.equal(drawer(h).size, 'min(1120px, 100vw)')
    const table = find(h.render(), 'Table').props
    assert.ok(table.scroll.x)
    assert.equal(table.columns.some(column => column.fixed), false)
    const metadata = table.columns.find(column => column.dataIndex === 'metadata')
    assert.equal(find(metadata.render('{invalid JSON'), 'pre').props.children[0], '{invalid JSON')
    assert.equal(find(metadata.render({service_name: '<script>literal</script>'}), 'pre').props.children[0], '{\n  "service_name": "<script>literal</script>"\n}')
    h.unmount()
})
