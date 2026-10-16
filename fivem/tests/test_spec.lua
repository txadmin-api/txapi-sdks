-- The generated lib/endpoints.lua matches sdks/spec/endpoints.json field by field.
test('generated endpoint table matches spec/endpoints.json', function()
    local spec = readSpec()
    eq(TxApi.API_VERSION, spec.apiVersion, 'API_VERSION')
    eq(TxApi.API_VERSION, '1.8.0')
    eq(TxApi.VERSION, '1.0.0')
    eq(#TxApi._spec.endpoints, #spec.endpoints, 'endpoint count')
    eq(#spec.endpoints, 67)
    for i, e in ipairs(spec.endpoints) do
        local g = TxApi._spec.endpoints[i]
        eq(g.id, e.id); eq(g.method, e.method, e.id); eq(g.path, e.path, e.id)
        eq(g.accountLevel, e.accountLevel == true, e.id)
        eq(g.permission, not TxApi.isNull(e.permission) and e.permission or nil, e.id)
        local q, p = {}, {}
        for _, x in ipairs(e.params) do
            if x['in'] == 'query' then q[#q + 1] = x.name .. ':' .. x.type .. ':' .. tostring(x.required) else p[#p + 1] = x.name end
        end
        local gq = {}
        for _, x in ipairs(g.query) do gq[#gq + 1] = x.name .. ':' .. x.type .. ':' .. tostring(x.required) end
        deq(gq, q, e.id .. ' query')
        eq(#g.pathParams, #p, e.id .. ' path params')
        local b, gb = {}, {}
        for _, x in ipairs(e.body) do b[#b + 1] = x.name .. ':' .. x.type .. ':' .. tostring(x.required) .. ':' .. tostring(x.maxLength) end
        for _, x in ipairs(g.body) do gb[#gb + 1] = x.name .. ':' .. x.type .. ':' .. tostring(x.required) .. ':' .. tostring(x.maxLength) end
        deq(gb, b, e.id .. ' body')
    end
end)

test('webhook event names are exported as constants', function()
    local spec = readSpec()
    deq(TxApi.WEBHOOK_EVENTS, { table.unpack(spec.webhookEvents) })
end)
