package TableAndSqlDemo

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.ExecutionEnvironment
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.{Table, TableEnvironment, Types}
import org.apache.flink.table.sinks.CsvTableSink
import org.apache.flink.table.sources.CsvTableSource
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem.WriteMode
/**
  * table api ----> 其实就是spark中DataFrame 的dsl风格
  * sql api ------> 就是允许我们写sql
  * 1:Structure of Table API and SQL Programs与我们该如何编写一个table SQL 程序呢
  *  All Table API and SQL programs for batch and streaming follow the same pattern.具体结构看代码
  *  创建(一个内部目录)程序的步骤
  *  1:选择我们要执行的环境
  *  2:创建表环境  通过TableEnvironment
  *     这个类是table API and SQL APT 的一个中心概念 它主要负责一下几点
  *     @1:Holding a reference to an ExecutionEnvironment or StreamExecutionEnvironment
  *     @2:Converting a DataStream or DataSet into a Table
  *     @3:Registering a Table in the internal catalog
  *     @4:Registering an external catalog
  *     @5:Executing SQL queries
  *     @6:Registering a user-defined (scalar, table, or aggregation) function
  *     还要注意一点的就是一个表有着自己的边界 这个边界就是TableEnvironment 不可能把两张不同环境的表结合起来进行一个
  *     相同的查询
  *     创建方法:通过静态方法  TableEnvironment.getTableEnvironment() 里面的参数为 StreamExecutionEnvironment
  *     或者 ExecutionEnvironment 和一个可选择的TableConfig
  *     关于TableConfig的作用  可以进行优化
  *     ***************
  *     STREAMING QUERY
  *     ***************
  *     val sEnv = StreamExecutionEnvironment.getExecutionEnvironment
  *     //create a TableEnvironment for streaming queries
  *     val sTableEnv = TableEnvironment.getTableEnvironment(sEnv)
  *     ***********
  *     BATCH QUERY
  *     ***********
  *     val bEnv = ExecutionEnvironment.getExecutionEnvironment
  *     //create a TableEnvironment for batch queries
  *     val bTableEnv = TableEnvironment.getTableEnvironment(bEnv)
  *  3:然后就开始注册我们的一张表
  *    一个TableEnvironment维护着一个通过名字注册的目录表 这个目录表的种类有两种 一个是输入表 一个是输出表
  *    输入表:与table api,SQL query 相关联 并且提供输入数据
  *       注册输入表的源头有很多
  *       存在的一个表对象 一般是Table API or SQL query的结果
  *       一个TableSource,访问外部数据,例如一个文件 数据库，消息系统
  *       CsvTableSource 这个类是flink为我们提供的一个源 我们可以用它来读取文件，然后注册成一张表
  *       我们就可以用sql进行相关的查询了
  *       DataStream 或者一个DataSet
  *    输出表:能够反射一个table api ，SQL query结果到外部系统
  *    CsvTableSink 这个类是flink为我们提供的一个sink 我们可通过他来注册一张输出表
  *    这个类需要三个参数 第一个路径 第二个为字段 第三个为字段的类型
  *    然后通过TableEnvironment注册成一张表
  *  4:创建一张表通过dsl风格或者SQL查询
  *  5:最后用insertInto方法将表插入到我们注册好的输出表中（tableSink）去
  *
  *
  *
  * Explaining a Table
  * The Table API provides a mechanism to explain the
  * logical and optimized query plans to compute a Table
  * 运用 TableEnvironment.explain(table)方法
  */
object TableAndSql {
  def main(args: Array[String]): Unit = {

    val environment: ExecutionEnvironment =ExecutionEnvironment.getExecutionEnvironment
    environment.setParallelism(1)
    val tableenvironment: TableEnvironment = TableEnvironment.getTableEnvironment(environment)
    /*tableenvironment.registerExternalCatalog("extCat", ...)*/
    val source: CsvTableSource = CsvTableSource.builder().path("C:\\Users\\Administrator\\Desktop\\table.txt")
      .field("id", Types.INT)
      .field("name", Types.STRING)
      .field("age", Types.INT).build()
     tableenvironment.registerTableSource("yuangong",source)
    val table: Table = tableenvironment.sqlQuery("select * from yuangong")
    val fieldNames: Array[String] = Array("id", "name", "age")
    val fieldTypes: Array[TypeInformation[_]] = Array(Types.INT, Types.STRING, Types.INT)
    tableenvironment.registerTableSink("out",fieldNames,fieldTypes, new CsvTableSink("C:\\Users\\Administrator\\Desktop\\tableResult.txt",",",1,WriteMode.OVERWRITE))
    table.insertInto("out")
    println(tableenvironment.explain(table))//返回的是一个字符串
    environment.execute()
  }
}

/**
  *
  * 结合上面没完成的代码 我们知道 注册一张表的源头有很多 上面的例子中我们已经完成一个了 就是通过TableSource
  * 来注册一张表
  * 我们还可以通过 DataSet 和 DataStream 来注册一张表 之后进行SQL查询 那么怎么实现呢
  * Table API and SQL Query 可以很简单的就集成和嵌入到DataSet 和 DataStream
  * 我们要导入隐式转换
  * DataSet: org.apache.flink.table.api.scala._
  * DataStream: org.apache.flink.api.scala._
  * 1:注册一个DataStream or DataSet成为一张表
  *   DataStream or DataSet作为一张表注册到TableEnvironment 结果表字段类型依赖于注册的DataStream or DataSet的数据类型
  *   像 Tuple pojo caseClass Flink的Row  那么究竟是怎么映射上的呢 内部细节如下
  *   一个数据类型映射到一个表字段嗓基本发生在两种方式 基于字段的位置 和 基于字段的名字
  *   1:字段的位置映射 Tuple caseClass Flink的Row 都是有一定顺序的 但pojo不是 所以pojo不能用于这个
  *   2:基于名字的映射 这个能够使用在任何的数据类型 包括pojo 全部的字段能够与名字相关联 并且能够重新命名他通过 as
  *   字段也能够进行重新排序
  *2:将一个 DataStream or DataSet转变成一张表  TableAndSql02
  *   除了直接注册一张表 我们也可以转变成一张表
  *   使用 tableEnvironment.fromDataStream(unit)方法
  *
  *
  *3:把一个表转变成一个 DataStream or DataSet
  *   在这种方式下 自定义一个DataStream or DataSet能够运行在 Table API And SQl Query 的结果上
  *   最简单的就是使用Row 但是也有其他的类型
  *     @ 转变一个表到DataStream 从一个表到流有两种模式
  *     append模式:
  *     retract模式:这个比较常用一些
  *     @ 转变一张表到DataSet
  *     convert the Table into a DataSet of Row
  *     val dsRow: DataSet[Row] = tableEnv.toDataSet[Row](table)(不要忘了加泛型)
  *
  *
  */
object TableAndSql01{
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setParallelism(1)
    //获取 表环境
    val tableenvironment: StreamTableEnvironment = TableEnvironment.getTableEnvironment(environment)
   //获取一个输入流
    /**
      * Error:(102, 98) could not find implicit value
      * for evidence parameter of type
      * org.apache.flink.api.common.typeinfo.TypeInformation[(String, Int)]val
      * unit: DataStream[(String, Int)] =
      * environment.socketTextStream("192.168.2.129", 9999).map(line => {
      *
      * 别忘了隐式转换
      */
    val unit: DataStream[(String, Int)] = environment.socketTextStream("192.168.2.129", 9999).map(line => {
      val strings: Array[String] = line.split(",")
     (strings(0), strings(1).trim.toInt)
    })
    //将流注册成一张表
    tableenvironment.registerDataStream("name",unit)
    val table: Table = tableenvironment.sqlQuery("select * from name")
    val fieldNames: Array[String] = Array("id", "age")
    val fieldTypes: Array[TypeInformation[_]] = Array(Types.STRING, Types.INT)
    tableenvironment.registerTableSink("out",fieldNames,fieldTypes, new CsvTableSink("C:\\Users\\Administrator\\Desktop\\tableResult.txt"))
    table.insertInto("out")
    environment.execute()
  }
}


object TableAndSql02{
  def main(args: Array[String]): Unit = {
    val environment: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    environment.setParallelism(1)
    //获取 表环境
    val tableenvironment: StreamTableEnvironment = TableEnvironment.getTableEnvironment(environment)
    val unit: DataStream[(String, Int)] = environment.socketTextStream("192.168.2.129", 9999).map(line => {
      val strings: Array[String] = line.split(",")
      (strings(0), strings(1).trim.toInt)
    })
    //将流转变成一张表
    val table: Table = tableenvironment.fromDataStream(unit)
    val fieldNames: Array[String] = Array("name", "age")
    val fieldTypes: Array[TypeInformation[_]] = Array(Types.STRING, Types.INT)
    tableenvironment.registerTableSink("out",fieldNames,fieldTypes, new CsvTableSink("C:\\Users\\Administrator\\Desktop\\tableResult.txt",",",1,WriteMode.OVERWRITE))
    table.insertInto("out")
    environment.execute()
  }
}

/**
  * 连接外部系统
  *     Flink’s Table API & SQL能够连接其他外部系统用来读和写 一个Table Source提供了存储在外部数据库的访问数据
  *     一个Table Sink 将一个表发射到外部系统中去 对于 source 和sink 的种类的依赖 有很多的形式
  *     (如果你想要实现自己的表的源头和结尾 那么请看  user-defined sources & sinks page.)
  *
  *连接者能够详细的定义通过以下两种方式
  *   1:已编程的方式使用一个在org.apache.flink.table.descriptors包下的Descriptor这个类对于Table & SQL API来说
  *   2:经由YAML配置文件来宣布对于 SQL Client
  *这两种方式不仅仅能够统一APIS 与 SQL Client 而且还可以在不更改实际声明的情况下获得更好的自定义实现的可扩展性。
  *每一个声明都与SQL CREATE TABLE 语句相似
  */
object TableAndSql03{

  def main (args: Array[String] ): Unit = {



}
}
