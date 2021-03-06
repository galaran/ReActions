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

package me.fromgate.reactions.actions;

import java.util.List;
import java.util.Map;
import me.fromgate.reactions.externals.RAWorldGuard;
import me.fromgate.reactions.util.Locator;
import me.fromgate.reactions.util.ParamUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ActionBlockFill extends Action {

	@Override
	public boolean execute(Player p, Map<String, String> params) {
		// loc1: loc2: / region / chance / block
		String istr = ParamUtil.getParam(params, "block", "");
		if (istr.isEmpty()) return false;
		ItemStack item = u().parseItemStack(istr);
		if ((item==null)||((!item.getType().isBlock()))){
			u().logOnce("wrongblockfill"+istr, "Failed to execute action BLOCK_FILL. Wrong block "+istr.toUpperCase());
			return false;
		}

		if (!ParamUtil.isParamExists(params, "region")&&!ParamUtil.isParamExists(params, "loc1","loc2")) return false;

		Location loc1=null;
		Location loc2=null;

		String regionName = ParamUtil.getParam(params, "region", "");
		if (!regionName.isEmpty()){
			List<Location> locs = RAWorldGuard.getRegionMinMaxLocations(regionName);
			if (locs.size()==2) {
				loc1=locs.get(0);
				loc2=locs.get(1);
			}
		} else {
			String locStr = ParamUtil.getParam(params, "loc1", "");
			if (!locStr.isEmpty()) loc1 = Locator.parseLocation(locStr, null);
			locStr = ParamUtil.getParam(params, "loc2", "");
			if (!locStr.isEmpty()) loc2 = Locator.parseLocation(locStr, null);
		}
		if (loc1==null||loc2==null) return false;
		
		if (!loc1.getWorld().equals(loc2.getWorld())) return false;
		int chance = ParamUtil.getParam(params, "chance", 100);
		fillArea (item,loc1,loc2,chance);
		return true;
	}

	@SuppressWarnings("deprecation")
	public void fillArea (ItemStack blockItem, Location loc1, Location loc2, int chance){
		Location min = new Location (loc1.getWorld(), Math.min(loc1.getBlockX(), loc2.getBlockX()),
				Math.min(loc1.getBlockY(), loc2.getBlockY()), Math.min(loc1.getBlockZ(), loc2.getBlockZ()));
		Location max = new Location (loc1.getWorld(), Math.max(loc1.getBlockX(), loc2.getBlockX()),
				Math.max(loc1.getBlockY(), loc2.getBlockY()), Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
		for (int x = min.getBlockX(); x<=max.getBlockX(); x++)
			for (int y = min.getBlockY(); y<=max.getBlockY(); y++)
				for (int z = min.getBlockZ(); z<=max.getBlockZ(); z++)
					if (u().rollDiceChance(chance)) {
						Block block =min.getWorld().getBlockAt(x, y, z); 
						block.setType(blockItem.getType());
						block.setData(blockItem.getData().getData());
					}

	}


}
