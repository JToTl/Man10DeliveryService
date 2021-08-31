package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.con
import ltotj.minecraft.man10deliveryservice.Main.Companion.disableItems
import ltotj.minecraft.man10deliveryservice.Main.Companion.disableWorlds
import ltotj.minecraft.man10deliveryservice.Main.Companion.available
import ltotj.minecraft.man10deliveryservice.Main.Companion.executor
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility
import ltotj.minecraft.man10deliveryservice.Utility.countAirPocket
import ltotj.minecraft.man10deliveryservice.Utility.createGUIItem
import ltotj.minecraft.man10deliveryservice.Utility.getDateForMySQL
import ltotj.minecraft.man10deliveryservice.Utility.setNBTInt
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

object OPCommand:CommandExecutor {

    private val mysql=MySQLManager(plugin, pluginTitle)

    private fun getAdminItemBox(order_id: Int): ItemStack {
        val result = mysql.query("select sender_name,wrapping,boxName,order_date from delivery_order where order_id=$order_id;")
                ?: return createGUIItem(Material.STONE, 1, "エラーアイテム")
        result.next()
        if (result.row == 0) {
            result.close()
            mysql.close()
            return createGUIItem(Material.STONE, 1, "エラー：オーダーが見つかりませんでした")
        }
        val wrapping = result.getString("wrapping")
        val item = createGUIItem(Material.valueOf(con.getString("itemBox.$wrapping.material")?:"CHEST"), 1, result.getString("boxName"), listOf("§e差出人：${result.getString("sender_name")}", "§e発送日：${result.getString("order_date")}", "§e受取日：${getDateForMySQL(Date())}","§4これはAdmin用のBOXです！"
                ,"§4AdminBoxをAdmin以外が開封した場合、","§4通常のBOXと同じ処理が行われます"))
        val meta = item.itemMeta
        meta.setCustomModelData(con.getInt("itemBox.$wrapping.customModelData"))
        result.close()
        mysql.close()
        item.itemMeta = meta
        setNBTInt(item, "order_id", order_id)
        setNBTInt(item,"admin_box",1)
        return item
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player)return true
        else if(!sender.hasPermission("mdelivery.admin")){
            sender.sendMessage("§4権限なし")
            return true
        }
        else if(args.isEmpty()){
            sender.sendMessage("使い方をここに入力")
            return true
        }
        when(args[0]){
            "banitem"->{
                if(sender.inventory.itemInMainHand.type!=Material.AIR) {
                    disableItems.add(sender.inventory.itemInMainHand.type.toString())
                    plugin.config.set("disableItems", disableItems)
                    plugin.saveConfig()
                    sender.sendMessage("${sender.inventory.itemInMainHand.type} を発送禁止リストに追加しました")
                }
            }
            "unbanitem"->{
                if(sender.inventory.itemInMainHand.type!=Material.AIR) {
                    disableItems.remove(sender.inventory.itemInMainHand.type.toString())
                    plugin.config.set("disableItems", disableItems)
                    plugin.saveConfig()
                    sender.sendMessage("${sender.inventory.itemInMainHand.type} を発送禁止リストから削除しました")
                }
            }
            "switch"->{
                available=!available
                plugin.config.set("enable", available)
                plugin.saveConfig()
                if(available){
                    sender.sendMessage("[$pluginTitle]を再開しました")
                }
                else{
                    sender.sendMessage("[$pluginTitle]を停止しました")
                }
            }
            "adminbox"->{
                if(args.size==1){
                    sender.sendMessage("オーダーIDを入力してください")
                    return true
                }
                val id=args[1].toIntOrNull()
                if(id==null||id<=0){
                    sender.sendMessage("オーダーIDは１以上の整数を指定してください")
                }
                else if(countAirPocket(sender.inventory)==0){
                    sender.sendMessage("インベントリ空きがありません")
                }
                else {
                    executor.execute {
                        sender.inventory.addItem(getAdminItemBox(id))
                    }
                }
            }
            "log"->{
                if(args.size<2){
                    sender.sendMessage("/mdop log <send,receive> <名前> (ページ数)")
                }
                else {
                    val page = if (args.size == 3) 1 else args[3].toIntOrNull() ?: 1
                    when (args[1]) {
                        "send" -> {
                            executor.execute {
                                val uuidCount = mysql.query("select count(owner_uuid) from player_status where owner_name='${args[2]}';")
                                        ?: return@execute
                                uuidCount.next()
                                if (uuidCount.row == 0) {
                                    uuidCount.close()
                                    mysql.close()
                                    sender.sendMessage("${args[2]}は見つかりませんでした")
                                    return@execute
                                }
                                val uuidResult= mysql.query("select owner_uuid from player_status where owner_name='${args[2]}';")!!
                                if (uuidCount.getInt("count(owner_uuid)")!=1){
                                    sender.sendMessage("${args[2]}という名前で登録されているユーザーは複数名存在します")
                                    sender.sendMessage("/mdop loguuid <sender,receive,box> <uuid> で再度検索してください")
                                    sender.sendMessage("ユーザー1,uuid:${uuidResult.getString("owner_uuid")}")
                                    var count = 0
                                    while (uuidResult.next()) {
                                        sender.sendMessage("ユーザー$count,uuid:${uuidResult.getString("owner_uuid")}")
                                        count++
                                    }
                                    uuidCount.close()
                                    uuidResult.close()
                                    mysql.close()
                                    return@execute
                                }
                                uuidResult.next()
                                val userUuid = uuidResult.getString("owner_uuid")
                                uuidCount.close()
                                uuidResult.close()
                                val result = mysql.query("select order_id,receiver_name,order_status,order_date from delivery_order where sender_uuid='$userUuid' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                                        ?: return@execute
                                result.next()
                                if (result.row == 0) {
                                    sender.sendMessage("§e送信ログが存在しません")
                                } else {
                                    sender.sendMessage(Utility.createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")}, 発送先:${result.getString("receiver_name")} " +
                                            ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                    var count = 0
                                    while (count < 9 && result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")}, 発送先:${result.getString("receiver_name")} " +
                                                ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                        count++
                                    }
                                    if (result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("[§a次のページへ]", "/mdop log send ${page+1}"))
                                    } else {
                                        sender.sendMessage("§a最後のページです")
                                    }
                                }
                                result.close()
                                mysql.close()
                            }
                        }
                        "receive"->{
                            executor.execute {
                                val uuidCount = mysql.query("select count(owner_uuid) from player_status where owner_name='${args[2]}';") ?: return@execute
                                uuidCount.next()
                                if (uuidCount.row == 0) {
                                    uuidCount.close()
                                    mysql.close()
                                    sender.sendMessage("${args[2]}は見つかりませんでした")
                                    return@execute
                                }
                                val uuidResult = mysql.query("select owner_uuid from player_status where owner_name='${args[2]}';")!!
                                if (uuidCount.getInt("count(owner_uuid)")!=1) {
                                    sender.sendMessage("${args[2]}という名前で登録されているユーザーは複数名存在します")
                                    sender.sendMessage("/mdop loguuid <sender,receive,box> <uuid> で再度検索してください")
                                    sender.sendMessage("ユーザー1,uuid:${uuidResult.getString("owner_uuid")}")
                                    var count = 0
                                    while (uuidResult.next()) {
                                        sender.sendMessage("ユーザー$count,uuid:${uuidResult.getString("owner_uuid")}")
                                        count++
                                    }
                                    uuidCount.close()
                                    uuidResult.close()
                                    mysql.close()
                                    return@execute
                                }
                                uuidResult.next()
                                val userUuid = uuidResult.getString("owner_uuid")
                                uuidCount.close()
                                uuidResult.close()
                                val result = mysql.query("select order_id,sender_name,order_status,receive_date from delivery_order where receiver_uuid='$userUuid' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                                        ?: return@execute
                                result.next()
                                if (result.row == 0) {
                                    sender.sendMessage("§e${10 * page}件以上のオーダーは存在しません")
                                } else {
                                    sender.sendMessage(Utility.createClickEventText_run("§9ID:${result.getInt("order_id")},受取日時:${result.getString("receive_date")},発送元:${result.getString("sender_name")}" +
                                            ",受け取り状況：${if(result.getBoolean("order_status"))"§d受け取り済" else "§4未受け取り"},§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                    var count = 0
                                    while (count < 9 && result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("§9ID:${result.getInt("order_id")},受取日時:${result.getString("receive_date")},発送元:${result.getString("sender_name")}" +
                                                ",受け取り状況：${if(result.getBoolean("order_status"))"§d受け取り済" else "§4未受け取り"},§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                        count++
                                    }
                                    if (result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("[§a次のページへ]", "/mdop log receive ${page + 1}"))
                                    }
                                }
                                result.close()
                                mysql.close()
                            }
                        }
                    }
                }
            }
            "loguuid" -> {
                if (args.size < 2) {
                    sender.sendMessage("/mdop loguuid <send,receive> <uuid> (ページ数)")
                } else {
                    val page = if (args.size == 3) 1 else args[3].toIntOrNull() ?: 1
                    when (args[1]) {
                        "send" -> {
                            executor.execute {
                                val result = mysql.query("select order_id,receiver_name,order_status from delivery_order where sender_uuid='${args[2]}' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                                        ?: return@execute
                                result.next()
                                if (result.row == 0) {
                                    sender.sendMessage("§e送信ログが存在しません")
                                } else {
                                    sender.sendMessage(Utility.createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")},発送先:${result.getString("receiver_name")} " +
                                            ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                    var count = 0
                                    while (count < 9 && result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")},発送先:${result.getString("receiver_name")} " +
                                                ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                        count++
                                    }
                                    if (result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("[§a次のページへ]", "/mdop log send ${page + 1}"))
                                    } else {
                                        sender.sendMessage("§a最後のページです")
                                    }
                                }
                                result.close()
                                mysql.close()
                            }
                        }
                        "receive" -> {
                            executor.execute {
                                val result = mysql.query("select order_id,sender_name,order_status,receive_date from delivery_order where receiver_uuid='${args[2]}' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                                        ?: return@execute
                                result.next()
                                if (result.row == 0) {
                                    sender.sendMessage("§e${10 * page}件以上のオーダーは存在しません")
                                } else {
                                    sender.sendMessage(Utility.createClickEventText_run("§9ID:${result.getInt("order_id")},受取日時:${result.getString("receive_date")},発送元:${result.getString("sender_name")}" +
                                            ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済" else "§4未受け取り"},§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                    var count = 0
                                    while (count < 9 && result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("§9ID:${result.getInt("order_id")},受取日時:${result.getString("receive_date")},発送元:${result.getString("sender_name")}" +
                                                ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済" else "§4未受け取り"},§e[クリックでAdminBoxを取得]", "/mdop adminbox ${result.getInt("order_id")}"))
                                        count++
                                    }
                                    if (result.next()) {
                                        sender.sendMessage(Utility.createClickEventText_run("[§a次のページへ]", "/mdop log receive ${page + 1}"))
                                    }
                                }
                                result.close()
                                mysql.close()
                            }
                        }
                    }

                }
            }

        }
        return true
    }
}