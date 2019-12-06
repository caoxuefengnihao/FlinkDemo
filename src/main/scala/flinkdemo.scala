
import java.{lang, util}

import org.apache.flink.api.common.functions.{GroupCombineFunction, GroupReduceFunction, RichFilterFunction, RichMapFunction}
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.java.aggregation.Aggregations
import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.util.Collector

import scala.collection.mutable
/**
  *
  * word count 案例
  * 实现步骤：
  * 1获取环境
  * 2加载数据
  * 3transformation
  * 4output
  * 5execute
  *
  *
  *
  *
  * GroupReduce on sorted groups
  * A group-reduce function accesses the elements of a group using an Iterable.
  * Optionally, the Iterable can hand out the elements of a group in a specified order.
  * In many cases this can help to reduce the complexity of a user-defined group-reduce function
  * and improve its efficiency.
  *
  * 通过聚合函数 我们还可以自定义聚合函数来代替reduce函数 因为reduce函数是将一个一个组里的数据都聚到一个组  然后在进行聚合操作
  * 这样会有一个非常不好的事情发生 那就是当数据量非常大的时候 会有大量额网络io 影响性能 那么我们就要自己定义一个函数 来先进行一个组的聚合操作
  * 在进行全局聚合 我们定义一个自己的类 并且实现接口
  * class MyCombinableGroupReducer
  * extends GroupReduceFunction[(String, Int),
  * String]with GroupCombineFunction[(String, Int), (String, Int)]
  */
object flinkdemo {
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    environment.setParallelism(5)
   val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\wc.txt")


   value.flatMap(_.split(" ")).map((_, 1)).groupBy(0)/*reduce((x,y) =>{(x._1,x._2+y._2)}).*/.reduceGroup(new MyCombinableGroupReducer).writeAsText("C:\\Users\\Administrator\\Desktop\\wcResult.txt",WriteMode.NO_OVERWRITE).setParallelism(1)

    /**
      * 结论 reduce 4 个结果文件 设置一个并行度之后 1个结果文件
      */
    /*reduceGroup((it:Iterator[(String,Int)],out:Collector[(String,Int)])=>{


      out.collect(it.reduce((x,y)=>{(x._1,x._2+y._2)}))
    })*//*.groupBy(line =>{line._1})
    .reduce( (x,y) =>{(x._1,x._2+y._2)}).print()*/
   // unit1.writeAsText("hdfs://dshuju01:9000/flink-wc")
   //value.flatMap(_.split(" ")).map((_,1)).join(value).where(0).equalTo(0).print()
      environment.execute()
}

  /**
    * Error:(15, 60) could not find implicit value for evidence parameter of
    * type org.apache.flink.api.common.typeinfo.
    * TypeInformation[String]val unit: GroupedDataSet[(String, Int)] =
    * value.flatMap(_.split(" ")).map((_,1)).groupBy(0)
    *
    * 出现这个问题是因为我们没有添加一个包
    * import org.apache.flink.streaming.api.scala._
    */
}
object Main {
  import org.apache.flink.api.scala.extensions._
  case class Point(x: Double, y: Double)
  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val ds = env.fromElements(Point(1, 2), Point(3, 4), Point(5, 6))
    //ds.filterWith{case Point(y ,_) => y>3}
    //这个就是简化的模式匹配
    ds.filterWith ({
      case Point(x, _) => x > 2
    }).print()
  }
}


/**
  *  lang.Iterable[(String, Int)] 通过点进去源码我们发现 GroupReduceFunction GroupCombineFunction 是java类
  *  而且lang.Iterable[(String, Int)] Collector[String] 也都是java的接口和迭代器
  *  所以我们想用scala的语言要导入一个包 import collection.JavaConverters._ 并且 用.asScala方法进行转变
  */

import collection.JavaConverters._
class MyCombinableGroupReducer
 extends GroupReduceFunction[(String, Int), String]with GroupCombineFunction[(String, Int), (String, Int)] {
  override def reduce(values: lang.Iterable[(String, Int)], out: Collector[String]): Unit = {
    var resum = 0
    var q :String = null
    for ((x,y) <- values.iterator().asScala) {
      println(x,y)
      resum+=y
      q = s"$x ----- $y"
    }
    out.collect(q)
    //out.collect(values.iterator().asScala.reduce((a,b)=>{(a._1,a._2+b._2)}))
  }
  override def combine(values: lang.Iterable[(String, Int)], out: Collector[(String, Int)]): Unit = {
    var sum = 0
    var t:(String,Int) = null
    for ((x,y) <- values.iterator().asScala) {
      println(x,y)
      sum+=y
      t = (x,sum)
    }
    out.collect(t)
   // out.collect(values.iterator().asScala.reduce((a,b)=>{(a._1,a._2+b._2)}))
  }
}


/**
  * 1:Aggregate on Grouped Tuple DataSet
  * There are some common aggregation operations that are frequently used.
  * The Aggregate transformation provides the following build-in aggregation functions:
  *   Sum,Min, and Max
  * The Aggregate transformation can only be applied on a Tuple DataSet
  *   and supports only field position keys for grouping.
  *
  * 案例演示 我们想要求出哪个单词出现的次数最多 怎么办  成功
  */

object test{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\wc.txt")
   value.flatMap(_.split(" ")).map((_,1)).groupBy(0).reduce((x,y)=>{(x._1,x._2+y._2)}).sortPartition(1,Order.DESCENDING).first(1).print()
  }
}

/**
  *
  * 输入的源有三种 最常用的为readTextFile readCsvFile 这两种
  */
object DataSet_source{

  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    /**
      *
      * filePath: String, 文件路径
      * lineDelimiter: String  "\n" 行分隔符
      * fieldDelimiter: String = ",", 每一行之间的分隔符
      * quoteCharacter: Character = null,
      * ignoreFirstLine: Boolean = false, 是否忽略文件首行
      * ignoreComments: String = null,
      * lenient: Boolean = false, 某一行解析失败是否放弃这一行
      * includedFields: Array[Int] = null, 包含的字段
      * pojoFields: Array[String] = null 与pojo对应的字段
      *
      */


    /**
      *Recursive Traversal of the Input Path Directory
      * Instead, only the files inside the base directory are read, while nested files are ignored.
      */
    // enable recursive enumeration of nested input files
    val env  = ExecutionEnvironment.getExecutionEnvironment

    // create a configuration object
    val parameters = new Configuration

    // set the recursive enumeration parameter  开启递归
    parameters.setBoolean("recursive.file.enumeration", true)

    // pass the configuration to the data source
    env.readTextFile("file:///path/with.nested/files").withParameters(parameters)

    /**
      * 读取压缩文件
      * Compression method	File extensions	Parallelizable
      *       DEFLATE	        .deflate	              no
      *       GZip	          .gz, .gzip	            no
      *       Bzip2	          .bz2	                  no
      *       XZ	            .xz	                    no
      */


  }
}

/**
  * 使用广播变量进行mapjoin 避免shuffle 提高性能
  * 跟spark一样 我们先将一个表中的数据广播到每个节点的内存中
  * 需求 求出每个班级每个学科的最高分数
  */
object broadvai{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    // 1. The DataSet to be broadcast
    val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\tt.txt")
    val value1: DataSet[(String, String, String)] = value.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1), strings(2))
    })
    val unit: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\ttt.txt")
    val unit1: DataSet[(String, String)] = unit.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1))
    })
    unit1.map(new RichMapFunction[(String,String) ,(String,String,String,String)] {
      var b: mutable.Buffer[(String, String, String)] =null
      // 3. Access the broadcast DataSet as a Collection
      override def open(parameters: Configuration): Unit = {
        /**
          * 这个地方要注意一下  我们要定义一下得到的广播变量的泛型 getBroadcastVariable[(String, String, String)
          * 而且 这个函数返回来的是java类型的集合 我们要通过 .asScala 方式将他转变成scala的集合 别忘了导包  import collection.JavaConverters._
          */
        b = getRuntimeContext.getBroadcastVariable[(String, String, String)]("broadcastSetName").asScala
      }
      var tuple :(String,String,String,String) = null
      override def map(value: (String, String)) = {
        for ((x,y,z) <- b.toArray ) {
         if (value._1 == x){
           tuple =(x,value._2,y,z)
         }
        }
        tuple
      }
    }).withBroadcastSet(value1,"broadcastSetName").groupBy(1, 2).maxBy(3).print()// 2. Broadcast the DataSet
  }
}

/**
  *  需求 求出每个班级每个学科的最高分数
  */
object joindemo {
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\tt.txt")
    val value1: DataSet[(String, String, String)] = value.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1), strings(2))
    })
    val unit: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\ttt.txt")
    val unit1: DataSet[(String, String)] = unit.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1))
    })
    value1.join(unit1).where(0).equalTo(0).map(x => {
      (x._1._1, x._1._2, x._1._3, x._2._2)
    }).groupBy(1, 3).maxBy(2).print()
  }
}

/**
  * 我们要有统计用户的访问次数 和 去除同一访问路径的的总次数
  * 其实我们先把他裁成两个问题
  * 1：用户的访问次数
  * 2：去除同一访问路径的的总次数
  *
  *
  */
object test01{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val unit: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\t.txt")
    val unit1: GroupedDataSet[((String, String), Int)] = unit.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1))
    }).map(x => {
      (x, 1)
    }).groupBy(0)
    unit1.reduce((x,y)=>{(x._1,x._2+y._2)}).writeAsText("C:\\Users\\Administrator\\Desktop\\result1.txt")
    unit.map(x => {
      val strings: Array[String] = x.split(",")
      (strings(0), strings(1),strings(3))
    }).distinct().map(x=>((x._1,x._2),1)).groupBy(0).reduce((x,y)=>{(x._1,x._2+y._2)}).writeAsText("C:\\Users\\Administrator\\Desktop\\result2.txt")
    environment.execute()
  }
}



object wordcount{

  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val unit: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\wc.txt")
    unit.flatMap(_.split(" ")).map((_,1)).groupBy(0).aggregate(Aggregations.SUM,1).print()
  }
}

/**
  * 需求 求出每个班级每个学科的最高分数 一个班级表 一个学科分数表
  */
object Mapjoin{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val unit: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\tt.txt")
    val value2: DataSet[Course] = unit.map(line => {
      val strings: Array[String] = line.split(",")
      Course(strings(0).toInt, strings(1), strings(2).toDouble)
    })
    val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\ttt.txt")
    val value1: DataSet[Classes] = value.map(line => {
      val strings: Array[String] = line.split(",")
      Classes(strings(0).toInt, strings(1))
    })
    value1.map(new RichMapFunction[Classes,(Int,String,String,Double)]{

      var b: mutable.Buffer[Course] = _
      override def open(parameters: Configuration): Unit = {
        b = getRuntimeContext.getBroadcastVariable[Course]("broadcastSetName").asScala
      }
      override def map(value: Classes) = {
        var tuple :(Int,String,String,Double) = null
        for (elem <- b.toArray) {
          if (value.id == elem.id){
            tuple =(value.id,elem.course,value.classes,elem.fenshu)
          }
        }
        tuple
      }
    }).withBroadcastSet(value2,"broadcastSetName").groupBy(1,2).maxBy(3).print()
  }
}
case class Course(var id:Int,var course:String,var fenshu:Double)
case class Classes(var id:Int,var classes:String)

/**
  * 我想求一共有多少个单词呢
  *
  */
object WordCound{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
   val value: DataSet[String] = environment.readTextFile("C:\\Users\\Administrator\\Desktop\\wc.txt")
    println(value.flatMap(_.split(" ")).map((_,1)).count())
  }
}


object test02{
  def main(args: Array[String]): Unit = {
    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    //environment.createInput()

  }
}