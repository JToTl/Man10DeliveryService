package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.available
import ltotj.minecraft.man10deliveryservice.Main.Companion.con
import ltotj.minecraft.man10deliveryservice.Main.Companion.disableWorlds
import ltotj.minecraft.man10deliveryservice.Main.Companion.executor
import ltotj.minecraft.man10deliveryservice.Main.Companion.playerLastSendingData
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility
import ltotj.minecraft.man10deliveryservice.Utility.getDateForMySQL
import ltotj.minecraft.man10deliveryservice.order.DeliveryOrder
import ltotj.minecraft.man10deliveryservice.order.DeliveryOrder.generateContainerSelectInv
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

object SendCommand :CommandExecutor{

    private val mysql = MySQLManager(plugin, pluginTitle)

    private fun getTimeFromDateTime(dateTime:String):Int {
        val list=dateTime.substring(11).split(Regex("[;:]"),0)
        return list[0].toInt()*3600+list[1].toInt()*60+list[2].toInt()?:0
    }

    private fun checkLastOrder(player:Player):Boolean{
        val lastTime= playerLastSendingData[player.uniqueId]?:return false
        val newTime = getDateForMySQL(Date()).toString()
        return lastTime.substring(0,10)==newTime.substring(0,10)&&abs(getTimeFromDateTime(lastTime)- getTimeFromDateTime(newTime))<con.getInt("sendInterval")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(args.isEmpty()){
            sender.sendMessage("§d/mdsend 相手の名前 でアイテムを送ることができます！")
        }
        else if(!available){
            sender.sendMessage("§4[$pluginTitle]はただいま停止中です")
        }
        else if(sender !is Player)return true
        else if(disableWorlds.contains(sender.world.name)){
            sender.sendMessage("§4${sender.world.name}で[$pluginTitle]を使うことはできません")
        }
        else if(args.size>1&&args[1].length>50){
            sender.sendMessage("ボックス名が５０文字を超えています")
        }
        else if(args[0].equals(sender.name,true)){
            sender.sendMessage("§4自分自身に荷物を送ることはできません！")
        }
        else if(checkLastOrder(sender)){
            sender.sendMessage("§4連続して荷物を送ることはできません！時間を置いてもう一度お試しください")
        }
        else {
            executor.execute {
                val result= mysql.query("select owner_uuid,delivery_amount from player_status where owner_name='${args[0]}';")?:return@execute
                result.next()
                if(result.row==0){
                    sender.sendMessage("${args[0]}は見つかりませんでした")
                    result.close()
                    mysql.close()
                    return@execute
                }
                result.close()
                mysql.close()
                Bukkit.getScheduler().runTask(plugin,Runnable{sender.openInventory(generateContainerSelectInv(args[0], if (args.size > 1) args[1] else "noName"))})
            }
        }
        return true
    }
}