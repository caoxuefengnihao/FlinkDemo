package Time

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

object WordEventTime {

  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val unit: DataStream[String] = environment.socketTextStream("127.0.0.1",9999)
    val value: DataStream[(String, (Long, Int))] = unit.filter(x => x != null && x != "").map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), (strings(1).toLong, 1))
    })
    val value2: WindowedStream[(String, (Long, Int)), String, TimeWindow] = value.assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[(String, (Long, Int))] {
      val maxOutOfOrderness = 2000L
      var currentMaxTimestamp: Long = _

      override def getCurrentWatermark: Watermark = {
        var watermark: Watermark = new Watermark(currentMaxTimestamp - maxOutOfOrderness)
        watermark
      }

      override def extractTimestamp(element: (String, (Long, Int)), previousElementTimestamp: Long): Long = {

        currentMaxTimestamp = Math.max(currentMaxTimestamp, element._2._1)
        element._2._1
      }
    }).keyBy(_._1).window(TumblingEventTimeWindows.of(Time.seconds(5)))
    value2.reduce((x,y)=>{(x._1,(x._2._1,x._2._2+y._2._2))}).map(x=>{(x._1,x._2._2)}).print()
    environment.execute("word")

  }
}
