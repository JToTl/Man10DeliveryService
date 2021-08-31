package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main.Companion.con
import ltotj.minecraft.man10deliveryservice.Main.Companion.vault
import ltotj.minecraft.man10deliveryservice.Utility.createGUIItem
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object LetterCommand:CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (args.isEmpty()) {
                sender.sendMessage("§e/mletter 一行目 二行目 ...")

            }
            else if(sender.inventory.itemInMainHand.type!=Material.AIR){
                sender.sendMessage("§e手に何も持たないでもう一度実行してください")
            }
            else if(vault.getBalance(sender.uniqueId)<con.getDouble("letter.price")){
                sender.sendMessage("§4お金が足りません！手紙を作るには§e${con.getDouble("letter.price")}円(電子マネー)§4が必要です")
            }
            else {
                vault.withdraw(sender,con.getDouble("letter.price"))
                val lore=ArrayList<String>()
                lore.add(("§e========================"))
                for(str in args){
                    lore.add("§e$str")
                }
                lore.add("§e========================")
                lore.add("§eby ${sender.name}")
                val letter=createGUIItem(Material.valueOf(con.getString("letter.material")!!),1,"§6§lmLetter",lore)
                sender.inventory.setItemInMainHand(letter)
            }
        }
        return true
    }

}