/*
 *   Copyright (C) 2016  MohiPE
 *   
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *   
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *   
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package kr.mohi.mohimine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;

import kr.mohi.mohimine.task.MineTask;

/**
 * 
 * @author MohiPE
 * @since 2016-5-11
 * 
 */
public class MohiMine extends PluginBase implements Listener {

	private ConfigSection mineDB, defaultProbability;
	private HashMap<String, HashMap<String, Position>> queue;
	private ConfigSection calcedSetting;

	@Override
	public void onEnable() {
		this.loadDB();
		this.mineCalc();
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getServer().getScheduler().scheduleRepeatingTask(new MineTask(this),
				this.getConfig().getInt("reset-tick", 20 * 60 * 30), true);
	}

	@Override
	public void onDisable() {
		this.saveAll();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) { // 플레이어가 아니면 무시
			return true;
		}
		if (command.getName().equals("mine")) { // /mine 명령어 사용시
			Player player = this.getServer().getPlayer(sender.getName());

			if (args[0] == null) {
				return false;
			}
			if (args[0].equals("pos1")) {
				this.setPos1(player);
				return true;
			}
			if (args[0].equals("pos2")) {
				this.setPos2(player);
				return true;
			}
			if (args[0].equals("set")) {
				if (args[1] == null)
					return false;
				this.setMine(player, args[1]);
				return true;
			}
			if (args[0].equals("del")) {
				if (args[1] == null)
					return false;
				this.delMine(args[1]);
				return true;
			}
		}

		return false;
	}

	/**
	 * Get Leveles
	 * 
	 * @return
	 */
	public Set<String> getMineNames() {
		return this.mineDB.getKeys();
	}

	/**
	 * Set pos1
	 * 
	 * @param player
	 */
	@SuppressWarnings("serial")
	public void setPos1(final Player player) {
		this.queue.put(player.getName(), new HashMap<String, Position>() {
			{
				put("pos1", player.getPosition());
			}
		});
	}

	/**
	 * Set pos2
	 * 
	 * @param player
	 */
	@SuppressWarnings("serial")
	private void setPos2(final Player player) {
		this.queue.put(player.getName(), new HashMap<String, Position>() {
			{
				put("pos2", player.getPosition());
			}
		});
	}

	/**
	 * 
	 * @param player
	 * @param name
	 */
	public void setMine(Player player, String name) {
		LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
		Position pos1 = this.queue.get(player).get("pos1");
		Position pos2 = this.queue.get(player).get("pos2");
		if (pos1 == null || pos2 == null) {
			return;
		}
		value.put("pos1", pos1.level.getId() + ":" + pos1.x + ":" + pos1.y + ":" + pos1.z);
		value.put("pos2", pos2.level.getId() + ":" + pos2.x + ":" + pos2.y + ":" + pos2.z);
		ConfigSection mine = new ConfigSection(value);
		this.mineDB.put(name, mine);
		this.saveDB(true);
		this.getServer().getScheduler().scheduleAsyncTask(new MineTask(this));
	}

	/**
	 * 
	 * @param pos1
	 * @param pos2
	 * @param name
	 */
	public void setMine(Position pos1, Position pos2, String name) {
		if (!(pos1.getLevel().getId() == pos2.getLevel().getId()))
			return;
		LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
		value.put("pos1", pos1.level.getId() + ":" + pos1.x + ":" + pos1.y + ":" + pos1.z);
		value.put("pos2", pos2.level.getId() + ":" + pos2.x + ":" + pos2.y + ":" + pos2.z);
		ConfigSection mine = new ConfigSection(value);
		this.mineDB.put(name, mine);
		this.saveDB(true);
		this.getServer().getScheduler().scheduleAsyncTask(new MineTask(this));

	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public Position execPos(String s) {
		String[] a = s.split(":");
		Level level = this.getServer().getLevel(Integer.parseInt(a[0]));
		int x = Integer.parseInt(a[1]);
		int y = Integer.parseInt(a[2]);
		int z = Integer.parseInt(a[3]);
		return new Position(x, y, z, level);
	}

	/**
	 * Delete mine
	 * 
	 * @param name
	 */
	public void delMine(String name) {
		if (!this.mineDB.containsKey(name)) {
			return;
		}
		this.mineDB.remove(name);
	}

	public void mineCalc() {
		Set<String> index = this.defaultProbability.getKeys();
		LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
		ConfigSection calcedSetting = new ConfigSection(value);
		for (String item : index) {
			String[] exploded = item.split("/");
			calcedSetting.put(item, Math.round(Integer.parseInt(exploded[1]) / Integer.parseInt(exploded[0])));
		}
		this.calcedSetting = calcedSetting;
	}

	public int randomBlock() {
		Set<String> index = this.calcedSetting.keySet();
		for (String item : index) {
			Random random = new Random();
			int rand = random.nextInt(this.calcedSetting.getInt(item) - 1) + 1;
			if (rand == 1)
				return Integer.parseInt(item);
		}
		return 1;
	}

	/**
	 * 
	 * @param level
	 */
	public void initMine(String name) {

	}

	public void loadDB() {
		this.saveDefaultConfig();
		this.mineDB = new Config(this.getDataFolder() + "/mine.json", Config.JSON, new ConfigSection()).getSections();
		this.saveResource("probability.json", false);
		this.defaultProbability = new Config(this.getDataFolder() + "/probability.yml", Config.YAML).getSections();

	}

	public void saveAll() {
		this.saveDB(false);
		this.saveConfig();
	}

	/**
	 * 
	 * @param async
	 */
	public void saveDB(Boolean async) {
		Config mineDB = new Config(this.getDataFolder() + "/mine.json", Config.JSON, new ConfigSection());
		mineDB.setAll(this.mineDB);
		mineDB.save(async);
	}

	/**
	 * 
	 * @param player
	 * @param message
	 */
	public void alert(CommandSender player, String message) {
		player.sendMessage(TextFormat.RED + "[MohiMine]" + " " + message);
	}

	/**
	 * 
	 * @param player
	 * @param message
	 */
	public void message(CommandSender player, String message) {
		player.sendMessage(TextFormat.BLUE + "[MohiMine]" + " " + message);
	}
}
