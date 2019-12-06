package Time

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.scala._
/**
  * 需求 假定用户每隔一秒钟统计前4秒的数据 并且将数据结果checkpoint
  *
  */
object FlinkCheckPoint {
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //在这设置checkpoint相关参数
    environment.setStateBackend(new FsStateBackend("file:///C:\\Users\\Administrator\\Desktop\\flinkcheckpoint"))
    environment.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    environment.getCheckpointConfig.setCheckpointTimeout(60000)
    environment.enableCheckpointing(2000)
    environment.getCheckpointConfig.setCheckpointInterval(1000)
    environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    val unit: DataStream[Event] = environment.addSource(new Source)
    val value: DataStream[Event] = unit.windowAll(SlidingProcessingTimeWindows.of(Time.seconds(4), Time.seconds(1))).process(new ProcessAllWindowFunction[Event, Event, TimeWindow] {
      override def process(context: Context, elements: Iterable[Event], out: Collector[Event]): Unit = {
        elements.foreach(line => {
          out.collect(line)
        })
      }
    })

    value.print()
    environment.execute()
  }

}
class Source extends RichSourceFunction[Event]{
  private val isRunning = true
  //这个方法是当需要事物进行回滚的时候就调用这个方法
  override def cancel(): Unit = ???
  override def run(ctx: SourceFunction.SourceContext[Event]): Unit = {
    while (isRunning){
      for (i<-1 to 10000){
        ctx.collect(Event(i,"zs"+i,"zheshiyigeceshi",i))
      }
      if (!isRunning){
        System.exit(0)
      }
    }
  }
}
case class Event(id:Int,name:String,info:String,count:Int)
