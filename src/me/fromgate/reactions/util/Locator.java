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


package me.fromgate.reactions.util;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.fromgate.reactions.RAUtil;
import me.fromgate.reactions.ReActions;
import me.fromgate.reactions.externals.RAWorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

public class Locator {
	private static HashMap<String,TpLoc> tports = new HashMap<String,TpLoc>();



	private static RAUtil u(){
		return ReActions.util;
	}

	public static Location parseLocation(String param, Location defaultLocation){
		Map<String,String> params = ParamUtil.parseParams(param,"loc");
		Location location = null;
		if (ParamUtil.isParamExists(params, "loc")) {
			String locStr = ParamUtil.getParam(params, "loc", "");
			location = Locator.getTpLoc(locStr);
			if (location == null) location = parseCoordinates(locStr);
		}
		boolean land = ParamUtil.getParam(params, "land", true);
		if (ParamUtil.isParamExists(params, "region")){
			location = getRegionLocation(ParamUtil.getParam(params, "region", ""),land);
			location = copyYawPitch(location, defaultLocation);
		} if (ParamUtil.isParamExists(params, "loc1","loc2")){
			Location loc1 = parseCoordinates(ParamUtil.getParam(params, "loc1", ""));
			Location loc2 = parseCoordinates(ParamUtil.getParam(params, "loc2", ""));
			if (loc1!=null&&loc2!=null)	{
				location = getCubeLocation (loc1,loc2,land);
				location = copyYawPitch(location, defaultLocation);
			}
		} if (ParamUtil.isParamExists(params, "radius")){
			int radius = ParamUtil.getParam(params, "radius", -1);
			if (radius>0){
				location = getRadiusLocation (location == null ? defaultLocation : location,radius,land);
				location = copyYawPitch(location, defaultLocation);
			}
		}
		return location == null ? defaultLocation : location;
	}

	public static Location copyYawPitch (Location targetLoc, Location sourceLoc){
		if (targetLoc==null||sourceLoc==null) return targetLoc;
		targetLoc.setYaw(sourceLoc.getYaw());
		targetLoc.setPitch(sourceLoc.getPitch());
		return targetLoc;
	}


	public static Location getCubeLocation (Location loc1, Location loc2, boolean land){
		List<Location> minmax = new ArrayList<Location>();
		minmax.add(new Location (loc1.getWorld(),  Math.min(loc1.getBlockX(), loc2.getBlockX()),
				Math.min(loc1.getBlockY(), loc2.getBlockY()),
				Math.min(loc1.getBlockZ(), loc2.getBlockZ())));
		minmax.add(new Location (loc1.getWorld(),  Math.max(loc1.getBlockX(), loc2.getBlockX()),
				Math.max(loc1.getBlockY(), loc2.getBlockY()),
				Math.max(loc1.getBlockZ(), loc2.getBlockZ())));
		return getMinMaxLocation (minmax,land);
	}


	public static Location getRegionLocation (String regionStr, boolean land){
		List<Location> minmax = RAWorldGuard.getRegionMinMaxLocations(regionStr);
		if (minmax.isEmpty()) return null;
		return getMinMaxLocation (minmax,land);
	}

	public static Location getRadiusLocation (Location center, int radius, boolean land){
		int x = u().getRandomInt(radius*2+1)-radius;
		int z = u().getRandomInt(radius*2+1)-radius;
		do {
			x = u().getRandomInt(radius*2+1)-radius;
			z = u().getRandomInt(radius*2+1)-radius;
		} while (radius>(Math.sqrt((double)x)+Math.sqrt((double)z)));

		List<Location> locs = new ArrayList<Location>();
		for (int y=Math.max(center.getBlockY()-radius, 0); y<=Math.min(center.getBlockY()+radius, center.getWorld().getMaxHeight()-1); y++){
			locs.add(new Location (center.getWorld(),center.getBlockX()+x,
					y, center.getBlockZ()+z));
		}
		return getEmptyOrLandedLocations (locs,land);
	}

	public static Location parseCoordinates (String strloc){
		Location loc = null;
		if (strloc.isEmpty()) return null;
		String [] ln = strloc.split(",");
		if (!((ln.length==4)||(ln.length==6))) return null;
		World w = Bukkit.getWorld(ln[0]);
		if (w==null) return null;
		for (int i = 1; i<ln.length; i++){
			if (!(u().isInteger(ln[i])||ln[i].matches("-?[0-9]+[0-9]*\\.[0-9]+")||ln[i].matches("-?[1-9]+[0-9]*"))) return null;
		}
		loc = new Location (w, Double.parseDouble(ln[1]),Double.parseDouble(ln[2]),Double.parseDouble(ln[3]));
		if (ln.length==6){
			loc.setYaw(Float.parseFloat(ln[4]));
			loc.setPitch(Float.parseFloat(ln[5]));
		}
		return loc;
	}

	private static boolean isEmptyLocation (Location loc){
		Block block = loc.getBlock();
		if (!block.isEmpty()) return false;
		if (!block.getRelative(BlockFace.UP).isEmpty()) return false;
		return true;
	}

	private static boolean isLandedLocation (Location loc){
		Block block = loc.getBlock();
		if (!block.isEmpty()) return false;
		if (block.getRelative(BlockFace.DOWN).isEmpty()) return false;
		if (!block.getRelative(BlockFace.UP).isEmpty()) return false;
		return true;
	}

	private static Location getRandomLocation (List<Location> locs){
		if (locs.isEmpty()) return null;
		return locs.get(ReActions.util.getRandomInt(locs.size()));
	}

	private static Location getEmptyOrLandedLocations (List<Location> locs, boolean land){
		List<Location> landLocs = new ArrayList<Location>();
		for (Location loc : locs){
			if (land){
				if (isLandedLocation (loc)) landLocs.add(loc);
			} else {
				if (isEmptyLocation (loc)) landLocs.add(loc);
			}
		}
		return landLocs.isEmpty() ? getRandomLocation (locs) : getRandomLocation (landLocs);
	}

	private static Location getMinMaxLocation (List<Location> minmax, boolean land){
		if (minmax.isEmpty()) return null;
		int x = minmax.get(0).getBlockX()+ReActions.util.getRandomInt(minmax.get(1).getBlockX()-minmax.get(0).getBlockX()+1);
		int z = minmax.get(0).getBlockZ()+ReActions.util.getRandomInt(minmax.get(1).getBlockZ()-minmax.get(0).getBlockZ()+1);
		List<Location> locations = new ArrayList<Location>();
		for (int y = minmax.get(0).getBlockY();y<=minmax.get(1).getBlockY();y++){
			locations.add(new Location (minmax.get(0).getWorld(), x,y,z));
		}
		return getEmptyOrLandedLocations (locations,land);
	}

	public static String locationToStringFormated(Location loc){
		if (loc == null) return "";
		DecimalFormat fmt = new DecimalFormat("####0.##");
		String lstr = loc.toString();
		try {
			lstr = "["+loc.getWorld().getName()+"] "+fmt.format(loc.getX())+", "+fmt.format(loc.getY())+", "+fmt.format(loc.getZ());
		} catch (Exception e){
		}
		return lstr;
	}

	public static String locationToString(Location loc){
		if (loc == null) return "";
		return loc.getWorld().getName()+","+
		trimDouble(loc.getX())+","+
		trimDouble(loc.getY())+","+
		trimDouble(loc.getZ())+","+
		(float)trimDouble(loc.getYaw())+","+
		(float)trimDouble(loc.getPitch());
	}

	public static double trimDouble(double d){
		int i = (int) (d*1000);
		return ((double)i)/1000;
	}

	/////////////////////////////////
	public static void saveLocs(){
		try {
			File f = new File (ReActions.instance.getDataFolder()+File.separator+"locations.yml");
			if (f.exists()) f.delete();
			if (tports.size()>0){
				f.createNewFile();
				YamlConfiguration lcs = new YamlConfiguration();
				for (String key : tports.keySet()){
					lcs.set(key+".world", tports.get(key).world);
					lcs.set(key+".x", tports.get(key).x);
					lcs.set(key+".y", tports.get(key).y);
					lcs.set(key+".z", tports.get(key).z);
					lcs.set(key+".yaw", tports.get(key).yaw);
					lcs.set(key+".pitch", tports.get(key).pitch);
				}
				lcs.save(f);
			}
		} catch (Exception e){
		}	
	}

	public static void loadLocs(){
		tports.clear();
		try {
			File f = new File (ReActions.instance.getDataFolder()+File.separator+"locations.yml");
			if (f.exists()){
				YamlConfiguration lcs = new YamlConfiguration();
				lcs.load(f);
				for (String key : lcs.getKeys(false))
					tports.put(key,new TpLoc (lcs.getString(key+".world"),
							lcs.getDouble(key+".x"),
							lcs.getDouble(key+".y"),
							lcs.getDouble(key+".z"),
							(float) lcs.getDouble(key+".yaw"),
							(float) lcs.getDouble(key+".pitch")));
			}
		} catch (Exception e){
		}			

	}


	public static boolean containsTpLoc(String locstr){
		return tports.containsKey(locstr);
	}

	public static Location getTpLoc(String locstr){
		if (tports.containsKey(locstr)) return tports.get(locstr).getLocation();
		return null;
	}


	public static int sizeTpLoc(){
		return tports.size();
	}

	public static void addTpLoc (String id, Location loc){
		tports.put(id, new TpLoc (loc));
	}

	public static boolean removeTpLoc(String id){
		if (!tports.containsKey(id)) return false;
		tports.remove(id);
		return true;
	}

	public static void printLocList(CommandSender p, int page, int lpp) {
		List<String> lst = new ArrayList<String>();
		for (String loc : tports.keySet()){
			lst.add("&3"+loc+" &a"+tports.get(loc).toString());
		}
		u().printPage(p, lst, page, "msg_listloc", "", true);
	}


}
