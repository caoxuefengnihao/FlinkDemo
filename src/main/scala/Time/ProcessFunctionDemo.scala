package Time
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
/**
  * 运用这个功能函数来求出一个窗口的出现的单词总数量
  *
  */
object ProcessFunctionDemo {

  def main(args: Array[String]): Unit = {

    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    val unit: DataStream[String] = environment.socketTextStream("127.0.0.1",9999)
    unit.map(line =>{
      val strings: Array[String] = line.split(",")
      (strings(0),strings(1).trim.toInt)
    }).keyBy(_._1).window(TumblingProcessingTimeWindows.of(Time.seconds(5))).process(new ProcessWindowFunction[(String,Int),Int,String,TimeWindow]{

      override def process(key: String, context: Context, elements: Iterable[(String, Int)], out: Collector[Int]): Unit = {
        var count = 0
        for (in <- elements) {
          count = count + 1
        }
       out.collect(count)
      }
    }).print()

environment.execute()
  }

}
