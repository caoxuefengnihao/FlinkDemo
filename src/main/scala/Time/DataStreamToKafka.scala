package Time

import java.util.Properties
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer09, FlinkKafkaProducer09}
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.util.Collector

/**
  *
  * Kafka消费者的相关代码   从一个topic中读取数据 在写回到 另一个topic中
  *
  * 成功！！！！！！
  *
  */
object DataStreamToKafka {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "192.168.2.129:9092")
    properties.setProperty("group.id", "test")
    properties.setProperty("zookeeper.connect", "192.168.2.129:2181")
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
   //topic: String, valueDeserializer: DeserializationSchema[T], props: Properties
    /**
      * 关于这个 valueDeserializer: DeserializationSchema[T] 有以下的解释
      *The Flink Kafka Consumer needs to know how to turn the binary data in Kafka into Java/Scala objects
      * Flink Kafka 消费者需要知道 如何将二进制的数据转化成Java/Scala对象
      */
    val unit: DataStream[String] = environment.addSource(new FlinkKafkaConsumer09[String]("pyg",new SimpleStringSchema(),properties))
    val value: DataStream[String] = unit.map(line => {
      val strings: Array[String] = line.split(",")
      ProcessedData(strings(0), strings(1))
    }).windowAll(TumblingProcessingTimeWindows.of(Time.seconds(3))).process(new ProcessAllWindowFunction[ProcessedData, String, TimeWindow] {
      override def process(context: Context, elements: Iterable[ProcessedData], out: Collector[String]): Unit = {
        var count = 0
        for (in <- elements) {
          count = count + 1
        }
        out.collect(count.toString)
      }
    })
      //将结果写回到Kafka中
    val myProducer = new FlinkKafkaProducer09( "mytopic", new SimpleStringSchema(),properties)
    // versions 0.10+ allow attaching the records' event timestamp when writing them to Kafka;
    // this method is not available for earlier Kafka versions
    //myProducer.setWriteTimestampToKafka(true)
    /**
      * 将产生的数据结果写回到 Kafka 中去
      * 消费者的addSource 是由执行环境调用的
      * 生产者的addSink 是由一个数据流的结果调用的
      *
      * */
    value.addSink(myProducer)
    unit.print()
    environment.execute()
  }
}
case class ProcessedData(regionalRequest: String, requestMethod: String)