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

package kr.mohi.mohimine.task;

import cn.nukkit.scheduler.AsyncTask;
import kr.mohi.mohimine.MohiMine;

/**
 * 
 * @author MohiPE
 * @since 2016-5-13
 *
 */
public class MineTask extends AsyncTask {

	private MohiMine plugin;

	public MineTask(MohiMine plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onRun() {
		for(String name : this.plugin.getMineNames()) {
			this.plugin.initMine(name);
		}
	}
}
