package ltotj.minecraft.man10deliveryservice

import ltotj.minecraft.man10deliveryservice.command.*
import ltotj.minecraft.man10deliveryservice.order.DeliveryOrder
import ltotj.minecraft.man10deliveryservice.order.ItemBox
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main : JavaPlugin(){

    companion object{
        lateinit var plugin:JavaPlugin
        lateinit var executor:ExecutorService
        lateinit var containerGUIName:String
        lateinit var containerSelectGUIName:String
        lateinit var con:FileConfiguration
        lateinit var vault:VaultManager
        var pluginTitle="Man10DeliveryService"
        var playerLastSendingData= ConcurrentHashMap<UUID,String>()
        lateinit var disableWorlds:MutableList<String>
        var disableItems=ArrayList<String>()
        var boxOpeningList=ArrayList<UUID>()
        var available=true

    }


    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        plugin=this
        executor= Executors.newCachedThreadPool()
        con=config

        containerGUIName= "§d§l送るアイテムを入れて羽をクリックしてください"
        containerSelectGUIName="§d§lラッピングを選択してください"
        vault=VaultManager(this)
        disableWorlds=con.getStringList("disableWorlds")
        available=con.getBoolean("enable")

        for(str in con.getStringList("disableItems")){
            disableItems.add(str)
        }

        server.pluginManager.registerEvents(DeliveryOrder,this)
        server.pluginManager.registerEvents(ItemBox,this)
        getCommand("mdrec")!!.setExecutor(ReceiveCommand)
        getCommand("mdsend")!!.setExecutor(SendCommand)
        getCommand("mdlog")!!.setExecutor(LogCommand)
        getCommand("mletter")!!.setExecutor(LetterCommand)
        getCommand("mdop")!!.setExecutor(OPCommand)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

}