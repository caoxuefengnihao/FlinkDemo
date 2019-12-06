package bin

import com.alibaba.fastjson.JSON


case class UserScan(
                     /**
                     {"browserType":"360浏览器","categoryID":8,"channelID":1,"city":"America",
                   "country":"America","entryTime":1544623260000,"leaveTime":1544634060000,
                   "network":"电信","produceID":1,"province":"America","source":"百度跳转","userID":6}
                       * */
                     var browserType:String ,
                     var categoryID:String ,
                     var channelID:String ,
                     var city:String ,
                     var country:String ,
                     var entryTime:String ,
                     var leaveTime:String ,
                     var network:String ,
                     var produceID:String ,
                     var province:String ,
                     var source:String ,
                     var userID:String
                   )

object UserScan{
  //将json串转换成对象
  def toBean(json:String):UserScan = {
    val value = JSON.parseObject(json)
    val browserType = value.get("browserType").toString
    val categoryID = value.get("categoryID").toString
    val channelID = value.get("channelID").toString
    val city = value.get("city").toString
    val country = value.get("country").toString
    val entryTime = value.get("entryTime").toString
    val leaveTime = value.get("leaveTime").toString
    val network = value.get("network").toString
    val produceID = value.get("produceID").toString
    val province = value.get("province").toString
    val source = value.get("source").toString
    val userID = value.get("userID").toString
    UserScan(browserType ,
      categoryID ,
      channelID ,
      city ,
      country ,
      entryTime ,
      leaveTime ,
      network ,
      produceID ,
      province ,
      source ,
      userID
    )
  }
}
