package Time


import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.aggregation.AggregationFunction.AggregationType
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.scala._
/**
  * 想要看看这个功能函数是怎么计算的
  * 例如 想要求出一个key 的平均值
  *
  */
object AggregateFunctionDemo {
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    val unit: DataStream[String] = environment.socketTextStream("192.168.2.129",9999)
    unit.map(line =>{
      val strings: Array[String] = line.split(",")
      (strings(0),strings(1).trim.toInt)
    }).keyBy(0).window(TumblingProcessingTimeWindows.of(Time.seconds(5))).aggregate(new AggregateFunction[(String,Int),(Int,Int),Int]{
      /**
        * 这个方法是将传入的值加到蓄能次中去
        *
        * @param value
        * @param accumulator
        * @return
        */
      override def add(value: (String, Int), accumulator: (Int, Int)) = {
        (value._2+accumulator._1,accumulator._2+1)
      }
      /**
        * 创建一个蓄能次
        * @return
        */
      override def createAccumulator() = (0,0)

      override def getResult(accumulator: (Int, Int)) = {accumulator._1/accumulator._2}

      /**
        * 这个方法是将两个蓄能次合并成一个蓄能次
        * @param a
        * @param b
        */
      override def merge(a: (Int, Int), b: (Int, Int)) = {(a._1+b._1,a._2+b._2  )}
    }).print()
environment.execute()
  }

}
