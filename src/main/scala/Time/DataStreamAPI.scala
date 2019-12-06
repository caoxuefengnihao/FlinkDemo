package Time

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.time.Time
/**
  * DataStream WordCount Demo
  *
  */
object DataStreamAPI {
  def main(args: Array[String]): Unit = {
    /**
      * 这与批处理的环境不一样了
      */
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val unit: DataStream[String] = environment.socketTextStream("192.168.2.129",9999)
    unit.flatMap(_.split(" ")).map((_,1)).keyBy(0).timeWindow(Time.seconds(5)).sum(1).print()
    environment.execute
  }
}
