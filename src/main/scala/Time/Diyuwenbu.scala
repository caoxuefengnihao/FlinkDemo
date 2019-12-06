package Time
import bin.UserScan
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.scala._
/**
  *业务三 频道的地域分布
  *
  */
object Diyuwenbu {
  def main(args: Array[String]): Unit = {
    val environment:ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    environment.readTextFile("C:\\Users\\Administrator\\Desktop\\diyu.txt").map(line =>{
      val scan: UserScan = UserScan.toBean(line)
      ((scan.channelID,scan.province),1)
    }).sortPartition(x=>x._1._1,Order.DESCENDING).print()



  }

}
