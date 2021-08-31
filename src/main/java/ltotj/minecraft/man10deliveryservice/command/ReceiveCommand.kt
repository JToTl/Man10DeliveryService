package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.executor
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility.countAirPocket
import ltotj.minecraft.man10deliveryservice.Utility.createGUIItem
import ltotj.minecraft.man10deliveryservice.Utility.getDateForMySQL
import ltotj.minecraft.man10deliveryservice.Utility.setNBTInt
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.min

object ReceiveCommand :CommandExecutor {

    private val mysql = MySQLManager(plugin, pluginTitle)

    private fun getItemBox(order_id: Int, sender_name: String, wrapping: String, boxName: String, order_date: String, receive_date: String): ItemStack {
        val item = createGUIItem(Material.valueOf(Main.con.getString("itemBox.$wrapping.material")?:"CHEST"), 1, boxName, listOf("§e差出人：$sender_name", "§e発送日：$order_date", "§e受取日：$receive_date"))
        val meta = item.itemMeta
        meta.setCustomModelData(Main.con.getInt("itemBox.$wrapping.customModelData"))
        item.itemMeta = meta
        setNBTInt(item, "order_id", order_id)
        return item
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if(Main.disableWorlds.contains(sender.world.name)){
            sender.sendMessage("§4${sender.world.name}で[${Main.pluginTitle}]を使うことはできません")
            return true
        }
        else if(!Main.available){
            sender.sendMessage("§4[${Main.pluginTitle}]はただいま停止中です")
            return true
        }
        val uuid = sender.uniqueId
        executor.execute {
            sender.sendMessage("§4§lお届け物を受け取っています・・・インベントリの操作をせずにお待ちください")
            val result = mysql.query("select order_id,sender_name,wrapping,boxName,order_date from delivery_order where receiver_uuid='$uuid' and order_status=false;")
            if (result == null) {
                println("[itemDelivery]データベース接続エラー")
                mysql.close()
                return@execute
            }
            result.next()
            if (result.row == 0) {
                sender.sendMessage("§eお届け物はないようです")
                result.close()
                mysql.close()
                return@execute
            }
            val items = HashMap<Int, ItemStack>()
            val receive_date = getDateForMySQL(Date())!!
            items[result.getInt("order_id")] = getItemBox(result.getInt("order_id"), result.getString("sender_name"), result.getString("wrapping"), result.getString("boxName"), result.getString("order_date"), receive_date)
            while (result.next()) {
                items[result.getInt("order_id")] = getItemBox(result.getInt("order_id"), result.getString("sender_name"), result.getString("wrapping"), result.getString("boxName"), result.getString("order_date"), receive_date)
            }
            result.close()
            mysql.close()
            if(Bukkit.getPlayer(sender.uniqueId)!=null) {
                val amount = min(countAirPocket(sender.inventory), items.size)
                for (i in 0 until amount) {
                    if (mysql.execute("update delivery_order set order_status=true,receive_date='${getDateForMySQL(Date())}' where order_id='${items.keys.elementAt(i)}';")) {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            sender.inventory.addItem(items.values.elementAt(i))
                        })
                    }
                }
                mysql.execute("update player_status set delivery_amount=${items.size - amount} where owner_uuid='${sender.uniqueId}'")
                sender.playSound(sender.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 2F)
                sender.sendMessage("§d${amount}個§a§lの配達物を受け取りました！")
            }
        }
        return true
    }
}
