package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.RequestOptions;

import java.util.concurrent.CompletableFuture;

/**
 * What a resource view needs to run calls: the shared core, view-level default options
 * and an optional response sink. Internal API.
 */
public final class Invoker {
    private final ClientCore core;
    private final RequestOptions defaults;
    private final ResponseSink sink;

    public Invoker(ClientCore core, RequestOptions defaults, ResponseSink sink) {
        this.core = core;
        this.defaults = defaults == null ? RequestOptions.none() : defaults;
        this.sink = sink == null ? ResponseSink.NONE : sink;
    }

    public ClientCore core() {
        return core;
    }

    public RequestOptions defaults() {
        return defaults;
    }

    public Invoker withDefaults(RequestOptions options) {
        return new Invoker(core, options == null ? defaults : options.over(defaults), sink);
    }

    public Invoker withSink(ResponseSink newSink) {
        return new Invoker(core, defaults, newSink);
    }

    public RequestOptions effective(RequestOptions perCall) {
        return perCall == null ? defaults : perCall.over(defaults);
    }

    public <T> T call(Call<T> call, RequestOptions perCall) {
        return core.execute(call, effective(perCall), sink);
    }

    public <T> CompletableFuture<T> callAsync(Call<T> call, RequestOptions perCall) {
        return core.executeAsync(call, effective(perCall), sink);
    }
}
