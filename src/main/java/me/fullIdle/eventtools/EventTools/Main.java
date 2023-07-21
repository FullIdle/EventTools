package me.fullIdle.eventtools.EventTools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class Main extends JavaPlugin implements Listener {
    public static Main main;
    public static ScriptEngine engine;

    @Override
    public void onEnable() {
        reload();
        main = this;

        getCommand("eventtools").setExecutor(this);
    }

    public void reload() {
        saveDefaultConfig();
        if (main != null){
            super.reloadConfig();
            unregisterEvent();
        }
        try {
            registerConfigEvent();
            registerCustomFileEvent();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }
    public void unregisterEvent(){
        HandlerList.unregisterAll((Plugin) this);
    }
    public void registerConfigEvent() throws ClassNotFoundException {
        List<String> list = getConfig().getStringList("RegisteredEvents");
        for (String key : list) {
            myRegisterEvent((Class<? extends Event>) Class.forName(key),this,
                    EventPriority.valueOf(getConfig().getString(key+".EventPriority")),
                    getConfig().getString(key+".ForgeEvent"),
                    getConfig().getString(key+"execute"),getConfig().getBoolean(key+".Asynchronous"));
        }
    }
    public void registerCustomFileEvent() throws ClassNotFoundException {
        File folder = new File(getDataFolder().toPath().resolve("listener").toAbsolutePath().toString());
        if (folder.mkdirs()) {
            getLogger().info("listener文件不存在,已重新创建");
        }else{
            getLogger().info("创建listener文件夹失败,或者已存在");
        }
        for (File file : folder.listFiles()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> list = config.getStringList("RegisteredEvents");
            for (String key : list) {
                myRegisterEvent((Class<? extends Event>) Class.forName(key),new MyListener(file),
                        EventPriority.valueOf(config.getString(key+".EventPriority")),
                        config.getString(key+".ForgeEvent"),
                        config.getString(key+"execute"),config.getBoolean(key+".Asynchronous"));
            }
        }
    }

    public void myRegisterEvent(Class<? extends Event> eventCls,Listener myListener,EventPriority eventPriority,String forgeEventCls,String execute,boolean asynchronous){
        getServer().getPluginManager().registerEvent(eventCls,
                myListener,eventPriority,
                (listener,event)->{
                    if (forgeEventCls!=null){
                        Class<?> c = null;
                        try {
                            c = Class.forName(forgeEventCls);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        Class<?> otherCls;
                        Object object;
                        try {
                            Method getForgeEvent = (event.getClass().getDeclaredMethod("getForgeEvent"));
                            object = getForgeEvent.invoke(event);
                            otherCls = object.getClass();
                        } catch (IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                        if (!c.isAssignableFrom(otherCls)){
                            return;
                        }else{
                            engine.put("event",object);
                        }
                    }else{
                        engine.put("event",event);
                    }
                    engine.put("plugin",main);
                    try {
                        engine.eval(execute);
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                },this,asynchronous);
        String listenerPath = myListener instanceof MyListener ? ((MyListener) myListener).getFile().getAbsolutePath():getDataFolder().toPath().resolve("config.yml").toAbsolutePath().toString();
        getLogger().info("§a注册了"+eventCls.getName()+"事件,监听器位置:"+listenerPath);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        main.reload();
        sender.sendMessage("§a配置已重载,请注意后台是否注册了你配置的事件");
        return false;
    }
}
