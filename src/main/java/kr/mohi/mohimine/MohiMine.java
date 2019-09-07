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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.SimpleCommandMap;
import cn.nukkit.event.Listener;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
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

	private ConfigSection mineDB = new ConfigSection(), defaultProbability = new ConfigSection();
	private Map<String, Position> queue = new HashMap<String, Position>();
	private ConfigSection calcedSetting = new ConfigSection();
	public static MohiMine instance;

	@Override
	public void onEnable() {
		this.getDataFolder().mkdirs();
		this.loadDB();
		this.registerCommands();
		MohiMine.instance = this;
		this.mineCalc();
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getServer().getScheduler().scheduleRepeatingTask(new MineTask(),
				this.getConfig().getInt("reset-tick", 20 * 60 * 30), true);
	}

	@Override
	public void onDisable() {
		this.saveAll();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.ingame"));
			return true;
		}
		if (command.getName().equalsIgnoreCase("mine")) {
			Player player = this.getServer().getPlayer(sender.getName());

			if (args.length < 1) {
				this.alert(sender, "/mine <pos1|pos2|set|del>");
				return true;
			}
			if (args[0].equalsIgnoreCase("pos1")) {
				this.setPos1(player);
				this.message(player, "Pos1을 설정하였습니다.");
				return true;
			}
			if (args[0].equalsIgnoreCase("pos2")) {
				this.setPos2(player);
				this.message(sender, "Pos2를 설정하였습니다.");
				return true;
			}
			if (args[0].equalsIgnoreCase("set")) {
				if (args[1] == null) {
					this.alert(sender, "/mine set <광산 이름>");
					return true;
				}
				this.plainMessage(sender, this.setMine(player, args[1]));
				return true;
			}
			if (args[0].equalsIgnoreCase("del")) {
				if (args[1] == null) {
					this.alert(sender, "/mine del <광산 이름>");
					return true;
				}
				this.plainMessage(sender, this.delMine(args[1]));
				return true;
			}
		}
		return false;
	}

	public void onPlayerQuit(PlayerQuitEvent event) {
		if (this.queue.containsKey(event.getPlayer().getName())) {
			this.queue.remove(event.getPlayer().getName());
		}
	}

	public static MohiMine getInstance() {
		return MohiMine.instance;
	}

	/**
	 * Get Levels
	 *
	 * @return
	 */
	public Set<String> getMineNames() {
		return this.mineDB.getKeys();
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public Map<String, Integer> getMinePos(String name) {
		if (!(this.mineDB.containsKey(name))) {
			return null;
		}
		Map<String, Integer> mine = new HashMap<String, Integer>();
		String[] pos1 = this.mineDB.getSection(name).getString("pos1").split(":");
		String[] pos2 = this.mineDB.getSection(name).getString("pos2").split(":");
		int pos1X = Integer.parseInt(pos1[1]);
		int pos1Y = Integer.parseInt(pos1[2]);
		int pos1Z = Integer.parseInt(pos1[3]);
		int pos2X = Integer.parseInt(pos2[1]);
		int pos2Y = Integer.parseInt(pos2[2]);
		int pos2Z = Integer.parseInt(pos2[3]);
		// Position pos1 = new Position(pos1[0], pos1[2], pos1[3],
		// this.getServer().getLevel(pos1[0]));
		// Position pos2 = new Position(pos2[0], pos2[2], pos2[3],
		// this.getServer().getLevel(pos2[0]));

		if (pos1X < pos2X) {
			mine.put("startX", pos1X);
			mine.put("endX", pos2X);
		} else {
			mine.put("startX", pos2X);
			mine.put("endX", pos1X);
		}

		if (pos1Y < pos2Y) {
			mine.put("startX", pos1Y);
			mine.put("endX", pos2Y);
		} else {
			mine.put("startX", pos2Y);
			mine.put("endX", pos1Y);
		}

		if (pos1Z < pos2Z) {
			mine.put("startX", pos1Z);
			mine.put("endX", pos2Z);
		} else {
			mine.put("startX", pos2Z);
			mine.put("endX", pos1Z);
		}

		mine.put("level", Integer.parseInt(pos1[0]));
		return mine;
	}

	/**
	 * Set pos1
	 *
	 * @param player
	 */
	private void setPos1(Player player) {
		this.queue.put(player.getName() + "pos1", player.getPosition());
	}

	/**
	 * Set pos2
	 *
	 * @param player
	 */
	private void setPos2(Player player) {
		this.queue.put(player.getName() + "pos2", player.getPosition());
	}

	/**
	 *
	 *
	 * @param player
	 * @param name
	 * @return
	 */
	public String setMine(Player player, String name) {
		if (this.mineDB.containsKey(name)) {
			return TextFormat.RED + "[MohiMine]" + " " + "이미 존재하는 광산 입니다.";
		}
		Position pos1 = this.queue.get(player.getName() + "pos1");
		Position pos2 = this.queue.get(player.getName() + "pos2");
		if (!(pos1.getLevel().getFolderName().equalsIgnoreCase(pos2.getLevel().getFolderName()))) {
			return TextFormat.RED + "[MohiMine]" + " " + "잘못된 위치입니다.";
		}
		if (pos1 == null || pos2 == null) {
			return TextFormat.RED + "[MohiMine]" + " " + "pos1 또는 pos2가 설정되어있지 않습니다.";
		}
		ConfigSection mine = new ConfigSection();
		mine.put("pos1", pos1.level.getFolderName() + ":" + (int) pos1.x + ":" + (int) pos1.y + ":" + (int) pos1.z);
		mine.put("pos2", pos2.level.getFolderName() + ":" + (int) pos2.x + ":" + (int) pos2.y + ":" + (int) pos2.z);
		mine.put("probability", this.calcedSetting);
		this.mineDB.put(name, mine);
		this.mineCalc();
		this.saveDB(false);
		this.getServer().getScheduler().scheduleAsyncTask(new MineTask());
		return TextFormat.BLUE + "[MohiMine]" + " " + "성공적으로 광산을 설정했습니다.";
	}

	/**
	 *
	 * @param pos1
	 * @param pos2
	 * @param name
	 * @return
	 */
	public String setMine(Position pos1, Position pos2, String name) {
		if (!(pos1.getLevel().getFolderName().equalsIgnoreCase(pos2.getLevel().getFolderName()))) {
			return TextFormat.RED + "[MohiMine]" + " " + "잘못된 위치입니다.";
		}
		if (this.mineDB.containsKey(name)) {
			return TextFormat.RED + "[MohiMine]" + " " + "이미 존재하는 광산 입니다.";
		}
		if (pos1 == null || pos2 == null) {
			return TextFormat.RED + "[MohiMine]" + " " + "pos1 또는 pos2가 설정되어있지 않습니다.";
		}
		ConfigSection mine = new ConfigSection();
		mine.put("pos1", pos1.level.getFolderName() + ":" + (int) pos1.x + ":" + (int) pos1.y + ":" + (int) pos1.z);
		mine.put("pos2", pos2.level.getFolderName() + ":" + (int) pos2.x + ":" + (int) pos2.y + ":" + (int) pos2.z);
		mine.put("probability", this.defaultProbability);
		this.mineDB.put(name, mine);
		this.mineCalc();
		this.saveDB(false);
		this.getServer().getScheduler().scheduleAsyncTask(new MineTask());
		return TextFormat.BLUE + "[MohiMine]" + " " + "성공적으로 광산을 설정했습니다.";

	}

	/**
	 *
	 * @param s
	 * @return
	 */
	public static Position execPos(String s) {
		String[] a = s.split(":");
		Level level = Server.getInstance().getLevel(Integer.parseInt(a[0]));
		int x = Integer.parseInt(a[1]);
		int y = Integer.parseInt(a[2]);
		int z = Integer.parseInt(a[3]);
		return new Position(x, y, z, level);
	}

	/**
	 * Delete mine
	 *
	 * @param name
	 * @return
	 */
	public String delMine(String name) {
		if (!this.mineDB.containsKey(name)) {
			return TextFormat.RED + "[MohiMine]" + " " + "존재하지 않는 광산입니다.";
		}
		this.mineDB.remove(name);
		this.saveDB(true);
		return TextFormat.BLUE + "[MohiMine]" + " " + "성공적으로 광산을 삭제했습니다.";
	}

	public void mineCalc() {
		ConfigSection calcedSetting = new ConfigSection();
		this.mineDB.getKeys().forEach(key -> {
			calcedSetting.put(key, new ConfigSection());
			this.mineDB.getSections(key).getSections("probability").getKeys().forEach(block -> {
				String[] exploded = this.mineDB.getSections(key).getSections("probability").getString(block).split("/");
				calcedSetting.getSection(key).put(block,
						Math.round(Integer.parseInt(exploded[1]) / Integer.parseInt(exploded[0])));
			});
			;
		});
		this.calcedSetting = calcedSetting;
	}

	/**
	 *
	 * @param name
	 * @return
	 */
	public Block randomBlock(String name) {
		Set<String> index = this.mineDB.getSections("name").getSection("probability").getKeys();
		for (String item : index) {
			Random random = new Random();
			int rand = random.nextInt(this.calcedSetting.getInt(item) - 1) + 1;
			if (rand == 1)
				return Block.get(Integer.parseInt(item));
		}
		return Block.get(1);
	}

	/**
	 *
	 * @param name
	 */
	public static void initMine(String name) {
		Map<String, Integer> pos = MohiMine.getInstance().getMinePos(name);
		if (pos == null) {
			return;
		}
		int startX = pos.get("startX");
		int endX = pos.get("endX");
		int startY = pos.get("startY");
		int endY = pos.get("endY");
		int startZ = pos.get("startZ");
		int endZ = pos.get("endZ");

		for (; startY <= endY; startY++) {
			for (; startZ <= endZ; startZ++) {
				for (; startX <= endX; startX++) {
					Server.getInstance().getLevel(pos.get("level")).setBlock(new Vector3(startX, startY, startZ),
							MohiMine.getInstance().randomBlock(name));
				}
			}
		}
	}

	public void loadDB() {
		this.saveDefaultConfig();
		this.mineDB = new Config(new File(this.getDataFolder(), "mine.json"), Config.JSON, new ConfigSection()).getSections();
		this.saveResource("probability.yml", false);
		this.defaultProbability = new Config(new File(this.getDataFolder(), "probability.yml"), Config.YAML)
				.getSections();
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
		Config mineDB = new Config(new File(this.getDataFolder(), "mine.json"), Config.JSON, new ConfigSection());
		mineDB.setAll(this.mineDB);
		mineDB.save(async);
	}

	/**
	 *
	 * @param name
	 * @param descript
	 * @param usage
	 * @param permission
	 */
	public void registerCommand(String name, String descript, String usage, String permission) {
		SimpleCommandMap commandMap = getServer().getCommandMap();
		PluginCommand<MohiMine> command = new PluginCommand<MohiMine>(name, this);
		command.setDescription(descript);
		command.setUsage(usage);
		command.setPermission(permission);
		commandMap.register(name, command);
	}

	public void registerCommands() {
		this.registerCommand("mine", "광산을 설정합니다.", "/mine <pos1|pos2|set|del>", "mohimine.command.*");
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

	/**
	 *
	 * @param player
	 * @param message
	 */
	public void plainMessage(CommandSender player, String message) {
		player.sendMessage(message);
	}
}
