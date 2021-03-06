/*  
 *  ReActions, Minecraft bukkit plugin
 *  (c)2012-2014, fromgate, fromgate@gmail.com
 *  http://dev.bukkit.org/server-mods/reactions/
 *    
 *  This file is part of ReActions.
 *  
 *  ReActions is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ReActions is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ReActions.  If not, see <http://www.gnorg/licenses/>.
 * 
 */

package me.fromgate.reactions;

import java.io.IOException;
import java.util.logging.Logger;

import me.fromgate.reactions.activators.Activator;
import me.fromgate.reactions.activators.Activators;
import me.fromgate.reactions.externals.LogHandler;
import me.fromgate.reactions.externals.RAProtocolLib;
import me.fromgate.reactions.externals.RACraftConomy;
import me.fromgate.reactions.externals.RAEffects;
import me.fromgate.reactions.externals.RAFactions;
import me.fromgate.reactions.externals.RARacesAndClasses;
import me.fromgate.reactions.externals.RATowny;
import me.fromgate.reactions.externals.RAVault;
import me.fromgate.reactions.externals.RAWorldGuard;
import me.fromgate.reactions.menu.InventoryMenu;
import me.fromgate.reactions.sql.SQLManager;
import me.fromgate.reactions.timer.Timers;
import me.fromgate.reactions.util.Delayer;
import me.fromgate.reactions.util.ItemUtil;
import me.fromgate.reactions.util.Locator;
import me.fromgate.reactions.util.RADebug;
import me.fromgate.reactions.util.Shoot;
import me.fromgate.reactions.util.Variables;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.palmergames.bukkit.towny.Towny;


public class ReActions extends JavaPlugin {
    String actionMsg="tp,grpadd,grprmv,townset,townkick,itemrmv,invitemrmv,itemgive,moneypay,moneygive"; //отображать сообщения о выполнении действий
    String language="english";
    boolean languageSave=false;
    boolean checkUpdates=false;
    boolean centerTpCoords = true;
    public int worlduardRecheck = 2;
    public int itemHoldRecheck = 2;
    public int itemWearRecheck = 2;
    public int sameMessagesDelay = 10;
    public boolean horizontalPushback = false;
    boolean enableProfiler = true;
    private boolean needUpdate;
    public static ReActions instance;
    public static RAUtil util;
    

    //разные переменные
    RAUtil u;
    Logger log = Logger.getLogger("Minecraft");
    private Cmd cmd;
    private RAListener l;
    private boolean towny_conected = false;

    public boolean isTownyConnected(){
        return towny_conected;
    }

    RADebug debug = new RADebug();

    public Activator getActivator(String id){
        return Activators.get(id);
    }

    @Override
    public void onEnable() {
        loadCfg();
        saveCfg();
        u = new RAUtil (this, languageSave, language, "react");
        u.initUpdateChecker("ReActions", "61726", "reactions", this.checkUpdates);
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        l = new RAListener (this);
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(l, this);
        pm.registerEvents(new InventoryMenu(), this);
        cmd = new Cmd (this);
        getCommand("react").setExecutor(cmd);
        instance = this;
        util = u;
        Timers.init();
        Activators.init();
        ItemUtil.init(this);
        RAEffects.init();
        RARacesAndClasses.init();
        RAFactions.init();
        Delayer.load();
        Variables.load();
        Locator.loadLocs();
        RAVault.init();
        RACraftConomy.init();
        RAWorldGuard.init();
        SQLManager.init();
        InventoryMenu.init();
        if (checkTowny()) towny_conected = RATowny.init();
        Bukkit.getLogger().addHandler(new LogHandler());
		if (Bukkit.getPluginManager().getPlugin("ProtocolLib")!=null)
			RAProtocolLib.connectProtocolLib();
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
        }
        
    }


    protected void saveCfg(){
        getConfig().set("general.language",language);
        getConfig().set("general.check-updates",checkUpdates);
        getConfig().set("reactions.show-messages-for-actions",actionMsg);
        getConfig().set("reactions.center-player-teleport",centerTpCoords);
        getConfig().set("reactions.region-recheck-delay",worlduardRecheck);
        getConfig().set("reactions.item-hold-recheck-delay",itemHoldRecheck);
        getConfig().getInt("reactions.item-wear-recheck-delay",itemWearRecheck);
        getConfig().set("reactions.horizontal-pushback-action",horizontalPushback );
        getConfig().set("reactions.need-file-update", needUpdate);
        getConfig().set("actions.shoot.break-block",Shoot.actionShootBreak);
        getConfig().set("actions.shoot.penetrable",Shoot.actionShootThrough);
        saveConfig();
    }

    protected void loadCfg(){
        language= getConfig().getString("general.language","english");
        checkUpdates = getConfig().getBoolean("general.check-updates",true);
        languageSave = getConfig().getBoolean("general.language-save",false);
        centerTpCoords = getConfig().getBoolean("reactions.center-player-teleport",true);
        actionMsg= getConfig().getString("reactions.show-messages-for-actions","tp,grpadd,grprmv,townset,townkick,itemrmv,itemgive,moneypay,moneygive");
        worlduardRecheck = getConfig().getInt("reactions.region-recheck-delay",2);
        itemHoldRecheck = getConfig().getInt("reactions.item-hold-recheck-delay",2);
        itemWearRecheck = getConfig().getInt("reactions.item-wear-recheck-delay",2);;
        horizontalPushback = getConfig().getBoolean("reactions.horizontal-pushback-action", false);
        needUpdate= getConfig().getBoolean("reactions.need-file-update", true);
        Shoot.actionShootBreak = getConfig().getString("actions.shoot.break-block",Shoot.actionShootBreak);
        Shoot.actionShootThrough = getConfig().getString("actions.shoot.penetrable",Shoot.actionShootThrough);
    }

    private boolean checkTowny(){
        Plugin twn = this.getServer().getPluginManager().getPlugin("Towny");
        return  ((twn != null)&&(twn instanceof Towny));
    }

    public RAUtil getUtils(){
        return this.u;
    }

    public boolean isCenterTpLocation(){
        return this.centerTpCoords;
    }

    public String getActionMsg(){
        return this.actionMsg;
    }

    public boolean needUpdateFiles() {
        return needUpdate;
    }

    public void setUpdateFiles (boolean update){
        if (update!= needUpdate){
            needUpdate = update;
            saveCfg();
        }
    }
}
