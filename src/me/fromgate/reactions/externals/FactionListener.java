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

package me.fromgate.reactions.externals;

import me.fromgate.reactions.event.EventManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.massivecraft.factions.event.FactionsEventCreate;
import com.massivecraft.factions.event.FactionsEventDisband;
import com.massivecraft.factions.event.FactionsEventMembershipChange;
import com.massivecraft.factions.event.FactionsEventRelationChange;
	
public class FactionListener implements Listener {

    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onFactionChange (FactionsEventMembershipChange event){
		EventManager.raiseFactionEvent(event.getUPlayer().getPlayer(), 
				event.getUPlayer().getFaction().isDefault() ? "default" : event.getUPlayer().getFactionName(),
						event.getNewFaction().isDefault() ? "default" : event.getNewFaction().getName());
	}
    
    
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
    public void onFactionCreate (FactionsEventCreate event){
    	EventManager.raiseFactionCreateEvent (event.getFactionName(), 
    			(event.getSender()!=null&&event.getSender() instanceof Player) ? (Player)event.getSender() : null);
    }
    
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
    public void onFactionDisband (FactionsEventDisband event){
    	EventManager.raiseFactionDisbandEvent (event.getFaction().getName(), 
    			(event.getSender()!=null&&event.getSender() instanceof Player) ? (Player)event.getSender() : null);
    }

    
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled = true)
	public void onRelationChange (FactionsEventRelationChange event){
    	EventManager.raiseFactionRelationEvent(event.getFaction().getName(),
    			event.getOtherFaction().getName(),
    			event.getFaction().getRelationWish(event.getOtherFaction()).name(),
    			event.getNewRelation().name());

    }

}
