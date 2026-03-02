# Publisher-Subscriber模式
Publisher发布一条订阅消息
Subscriber订阅一条订阅消息
Subcription控制器用于将发布者发布的订阅消息推送到订阅者，推送的方式是声明式推送，即onNext x 0..N [onError | onComplete]整个过程是异步且非阻塞的。
生产者和消费者之前的订阅消息🈶特殊的process类型转换器特殊处理
[图片]
整个操作流程可以被描述为（我的理解）：
发布者发布一条订阅消息，通过声明式的方式推送给订阅者，但是在subscribe操作真正执行之前什么也不会发生，一旦订阅，一个subscriber对象的链就会被创建（自下而上到第一个发布者），也就是说，订阅操作是连接发布订阅之间的数据流，调度器可以决定一个subscriber对象的链在哪条线程上创建。

在Reactor中，操作符是我们流水线类比中的工作站。每个操作符添加行为到 Publisher 中，并将上一步的 Publisher 包装到新的实例中。因此，整个链被链接在一起，数据源于第一个 Publisher 沿着链向下移动，并通过每个链接进行转换。最终，Subscriber 完成处理。记住，正如我们很快会看到的，在 Subscriber 订阅 Publisher 之前，什么都不会发生。
# Publisher操作符Flux和Mono
## Flux和Mono定义
1、Flux，一个包含0-N个元素的异步序列
由Publisher发布0-N个异步序列，可以被 onComplete 或 onError 信号选择性终止，没有onError且有onComplete，返回一个空的有序集合，没有onError且没有onComplete，返回一个空的无序集合。
Flux.range(5, 3);  第一个参数是范围的开始，第二个参数是产生元素的个数
[图片]
2、Mono，一个包含0-1结果的异步序列
Mono是Flux的一个子集，可以被 onComplete 或 onError 信号选择性终止。可以用Mono<Void>来表示无值的异步处理。
## 声明一个Flux或Mono
1、简单创建一个Flux<>或者Mono<>的序列，可以枚举他们：
```java
Flux<String> data = Flux.just("foo", "bar", "foobar");
Mono<String> noData = Mono.empty();
Mono<String> data = Mono.just("foo");
```
或者放入集合中:
```java
List<String> seq = Arrays.asList("foo", "bar", "foobar");
Flux<String> data = Flux.fromIterable(seq);
```
2、以编程方式创建序列
通过编程式创建Flux或者Mono序列的特点是：暴露一个对外的API来触发事件（称之为sink的事件）
同步 generate
用于synchronize和一对一发射，每个SychroniousSink的回调函数只能执行一次next方法，generate函数可以泛化为BiFunction<S, SynchronousSink<T,S>, S> 其中，S为初始状态，SynchronousSink<T,S>决定用状态来发出什么、什么时候停止，S为返回的新状态
Flux<String> flux = Flux.generate(
        () -> 0, 初始状态为0
                (state, sink) -> { publisher发布的订阅要执行哪些操作
      sink.next("3 x " + state + " = " + 3*state); 
      if (state == 10) sink.complete(); 
      return state + 1; 返回的新状态 
    });
# Subscription
一旦订阅，一个订阅者subsriber的对象链就会创建出来，沿着链向上到第一个生产者。记住订阅之前什么都不会发生。
[图片]
```
在Reactor中，当你用链接操作符时，你可以根据需要在内部封装尽可能多的 Flux 和 Mono 实现。一旦你订阅了，一个 Subscriber 对象的链就会创建出来，向后（沿着链向上）到第一个生产者。这实际上是对你隐藏掉的。你能看到的只是外层到 Flux (或 Mono)和 Subscription，但是这些中间的操作符的订阅才是真正的工作。
```
## 订阅subscribe()/取消订阅dispose()
形成不同的回调组合用于发布者订阅
订阅能够将发布者和订阅者进行绑定，从而触发整个链中的流数据。
```java
subscribe(); 订阅并触发序列
subscribe(Consumer<? super T> consumer); 处理每个值
subscribe(Consumer<? super T> consumer,
Consumer<? super Throwable> errorConsumer); 处理每个值和异常
subscribe(Consumer<? super T> consumer,
Consumer<? super Throwable> errorConsumer,
Runnable completeConsumer); 处理每个值和异常，在序列完成后运行一些代码
subscribe(Consumer<? super T> consumer,
Consumer<? super Throwable> errorConsumer,
Runnable completeConsumer,
Consumer<? super Subscription> subscriptionConsumer);  处理每个值和异常和成功，继续处理Subscription
```
## Scheduler 调度器
在Reactor中，执行模型和执行的位置由使用的 Scheduler 决定。类似ExecutorService的调度职责，可以多线程执行。通过 Schedulers.fromExecutorService(ExecutorService) 创建一个 Scheduler。
在Reactor中，当你用链接操作符时，你可以根据需要在内部封装尽可能多的 Flux 和 Mono 实现。一旦你订阅了，一个 Subscriber 对象的链就会创建出来，向后（沿着链向上）到第一个生产者。这实际上是对你隐藏掉的。你能看到的只是外层到 Flux (或 Mono)和 Subscription，但是这些中间的操作符的订阅才是真正的工作。
## SubscribeOn 操作符
特点：一旦创建改变了状态作用于链的thread，而是将其转移到Scheduler中的线程
以代码内部XExecutorAsync.java->listExecute为例：
1、对于序列中的每个元素，我们异步处理（在调用的 flatMap 函数内部）两次。
2、一旦订阅，一个subscriber对象的链就会创建出来，并且在自己的线程上执行（即每个X产品都创建了自己的线程和维护自己的上下文），同时沿着链向上到第一个生产者。
```java
private Mono<ISubContext> listExecute(XContext context, IXFactory factory,
long timeout) {
return Mono.just(factory.getProductType().value())
.flatMap(productType -> {
XExecutorAsync xExecutor = new XExecutorAsync(factory);
return xExecutor.listExecute(context)
.timeout(Duration.ofMillis(timeout))
.onErrorResume(e -> {})
.doOnTerminate(() -> {});
})
.subscribeOn(getScheduler(factory.getProductType().value()));
}
private Scheduler getScheduler(String productType) {
String name = productType + ExecutorType.REACTOR_XENGINE_SCHEDULER.getValue();
return Schedulers.fromExecutor(CatAsync.wrap(ExecutorServiceMgr.getExecutorService(name)));
}
```

⚠️注意：
1、虽然调用链顺序处理，但 Reactor 框架在不同 flatMap 间可能切换线程，使得 Context 对象跨线程访问，内层非线程安全的HashMap就成为了并发隐患

学习资料：https://easywheelsoft.github.io/reactor-core-zh/index.html#intro-reactive