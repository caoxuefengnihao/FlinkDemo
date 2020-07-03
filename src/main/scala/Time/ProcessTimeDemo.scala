package Time
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
object ProcessTimeDemo {
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    /**
      * Note that in order to run this example in event time,
      * the program needs to either use sources that
      * directly define event time for the data and emit watermarks themselves,
      * or the program must inject a Timestamp Assigner & Watermark Generator after the sources.
      */
    environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    val unit: DataStream[String] = environment.socketTextStream("127.0.0.1",9999)
    val value: DataStream[(Long, String, String, Double)] = unit.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0).trim.toLong, strings(1), strings(2), strings(3).trim.toDouble)
    }).keyBy(2).window(TumblingProcessingTimeWindows.of(Time.seconds(3))).max(3).setParallelism(1)
    value.print()
    environment.execute()
  }
}
