package Time

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
/**
  *
  * 编写关于event time 的流处理程序的相关步骤
  * 1:获取一个执行环境
  * 2:通过执行环境  获取我们想要对流处理的一个时间情况
  * 3:分配时间戳和水印 There are two ways to assign timestamps and generate watermarks:
  *   @1直接从数据源头来分配
  *   @2经由时间戳分配器/水印产生者:
  * Timestamp assigners让一个流产生一个新的流 里面的元素呢 都带有一个时间戳和水印
  * 如果原始的那个数据流有了时间戳和水印  timestamp assigner 会将他们重写
  *   在Flink当中, 时间戳分配器也定义了水印的发射 在这有两种水印产生 运用两个类
  *   With Periodic Watermarks:AssignerWithPeriodicWatermarks
  *   With Punctuated Watermarks:AssignerWithPunctuatedWatermarks
  *   @3:还可以自定义 只要实现AssignerWithPeriodicWatermarks,AssignerWithPunctuatedWatermarks的其中一个接口就行
  *   The remainder of this section presents the main
  *   interfaces a programmer has to implement in
  *   order to create her own timestamp extractors/watermark emitters.
  *   @4:关于在并行度不唯一（例如kafka）水印的处理问题  Watermarks in Parallel Streams
  *  源函数的每个并行子任务都会产生各自的一个水印 例如 kafka的分区
  *
  *
  *
  *
  *
  * 4:
  * 5:
  * 6:
  * 7:
  * 8:
  * 9:
  *
  *
  *
  *
  *
  *
  *
  *
  * 还有关于窗口的问题没有解决
  * 窗口是无限流的核心 我们的计算环境都在这个窗口当中  那么开窗口分为 两种
  * 1：如果上面的这个流是一个有键值对的流 那么我们应该用 window 这个transformation 来开窗
  * Keyed Windows  如果你的流中有键 那么就可以允许你以并行的方式进行窗口计算 全部的元素中相同的key将要发送到相同的并行任务中
  * stream
       .keyBy(...)               <-  keyed versus non-keyed windows
       .window(...)              <-  required: "assigner"
      [.trigger(...)]            <-  optional: "trigger" (else default trigger)
      [.evictor(...)]            <-  optional: "evictor" (else no evictor)
      [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
      [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
       .reduce/aggregate/fold/apply()      <-  required: "function"
      [.getSideOutput(...)]      <-  optional: "output tag"
  * 2：如果上面的这个流不是一个键值对的流 那么我们应该用 windowAll 这个transformation 来开窗
  * 窗口逻辑将会被执行以单一任务的方式 并行度为一
  * stream
       .windowAll(...)           <-  required: "assigner"
      [.trigger(...)]            <-  optional: "trigger" (else default trigger)
      [.evictor(...)]            <-  optional: "evictor" (else no evictor)
      [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
      [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
       .reduce/aggregate/fold/apply()      <-  required: "function"
      [.getSideOutput(...)]      <-  optional: "output tag"
  * 我们先来了解一下窗口的生命周期
  * 当第一个元素到达上一个窗口末尾的时候 那么下一个窗口就会开启
  * 窗口在最后一个元素通过用户定义的最后的时间戳加上用户定义的水印 那么这个窗口就会移除
  * 另外 每一个窗口都会有一个trigger 和一个函数(ProcessWindowFunction, ReduceFunction, AggregateFunction or FoldFunction)
  * 而且 这个trigger 也能够清除一个窗口的数据 但是不能够清除窗口的元数据信息
  *
  * 2：当确定了你的流中有没有键值对之后  我们开始定义一个窗口注册者(WindowAssigner) 这个是通过 window方法 和 windowAll 方法调用的
  * 他主要负责把 将要来的哪个元素分配到一个或多个窗口中去
  * Flink使用TimeWindow这个时 基于时间的窗口有一个方法来查询开始与结束时间戳 还有一个maxTimestamp() 这个方法
  * 来确定在给定的一个窗口中能够允许的最大的时间戳
  * flink 为我们事先定义了几个WindowAssigner  tumbling windows sliding windows session windows 用户也可以自定义
  *  tumbling windows：当定义了这个时 当前的窗口会被评估 并且按照间隔来开窗
  *
  *  session windows 没有覆盖窗口 也没有一个固定的开始和结束时间
  *
  *
  * 3:当我们确定窗口注册者之后 我们就需要在每个窗口上确定我们想要执行的计算函数了，
  * 这个是由窗口函数确定的(Window Functions)
  * ReduceFunction,
  * AggregateFunction,
  * FoldFunction
  * ProcessWindowFunction
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  *
  * 关于 落后的事件的解决方案 Allowed Lateness
  * 在event time 里 肯定有落后的事件 那么这个落后的事件怎么处理呢?
  * flink 默认的处理规则就是 当这个水印通过了一个窗口的末端  那么这个事件就丢掉了 不进行处理
  *
  *
  *
  *
  *
  *
  *
  */

object EventTimeDemo {
  def main(args: Array[String]): Unit = {
    //获取执行环境
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //通过执行环境  获取我们想要对流处理的一个时间情况
    /**
      * TimeCharacteristic 这个类是一个枚举类 获取我们想要对流处理的一个时间情况
      * ProcessingTime,
      * IngestionTime,
      * EventTime;
      */

    environment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    environment.setParallelism(1)
    val unit: DataStream[String] = environment.socketTextStream("192.168.2.129",9999)
    /**
      * With Punctuated Watermarks
      * AssignerWithPeriodicWatermarks:
      * 这个产生者产生的水印 假定那些元素是有循序的到达的,
      * 但是仅仅是大部分. The latest elements for a certain timestamp t will arrive
      * at most n milliseconds after the earliest elements for timestamp t.
      */
    val value: DataStream[String] = unit.assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[String] {
      val maxOutOfOrderness = 2000L // 3.5 seconds
      var currentMaxTimestamp: Long = _
      override def getCurrentWatermark: Watermark = new Watermark(currentMaxTimestamp-maxOutOfOrderness)
      override def extractTimestamp(t: String, l: Long): Long = {
        val strings: Array[String] = t.split(",")
        currentMaxTimestamp = {if (currentMaxTimestamp< strings(0).toLong){currentMaxTimestamp = strings(0).toLong};currentMaxTimestamp}
        strings(0).toLong
      }
    })
    /**
      * AssignerWithPunctuatedWatermarks
      * 这个目前还没有弄懂
      */
    /**
      * 案例 来统计3秒内商品拍卖的最大值
      *
      * 实验结果
      * (1527911155000,boos1,pc1,100.0）
      * (1527911156000,boos2,pc1,200.0)
      *
      * 为什么只出现了两条记录呢？
      * 这是因为在时间时间当中 是左闭右开的
      */
    value.map(line => {
      val strings: Array[String] = line.split(",")
      (strings(0).toLong,strings(1),strings(2),strings(3).toDouble)
    }).keyBy(1).window(TumblingEventTimeWindows.of(Time.seconds(3))).max(3).print()
    environment.execute()
  }

}
//class AverageAggregate extends AggregateFunction[(String, Long), (Long, Long), Double]