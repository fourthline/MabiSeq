/*
 * Copyright (C) 2013 たんらる
 */

package fourthline.mabiicco;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

import fourthline.mabiicco.midi.MabiDLS;
import fourthline.mabiicco.ui.PianoRollView;

public final class MabiIccoProperties {
	private final Properties properties = new Properties();
	private final String configFile = "config.properties";

	private static final MabiIccoProperties instance = new MabiIccoProperties();

	public static MabiIccoProperties getInstance() {
		return instance;
	}

	private MabiIccoProperties() {
		try {
			properties.load(new FileInputStream(configFile));
		} catch (InvalidPropertiesFormatException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void save() {
		try {
			properties.store(new FileOutputStream(configFile), "");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getRecentFile() {
		String str = properties.getProperty("app.recent_file", "");
		return str;
	}

	public void setRecentFile(String path) {
		properties.setProperty("app.recent_file", path);
		save();
	}

	public List<File> getDlsFile() {
		String str = properties.getProperty("app.dls_file", MabiDLS.DEFALUT_DLS_PATH);
		String filenames[] = str.split(",");
		ArrayList<File> fileArray = new ArrayList<>();
		for (String filename : filenames) {
			fileArray.add(new File(filename));
		}
		return fileArray;
	}

	public void setDlsFile(File fileArray[]) {
		StringBuilder sb = new StringBuilder();
		for (File file : fileArray) {
			sb.append(file.getPath()).append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		properties.setProperty("app.dls_file", sb.toString());
		save();
	}

	public boolean getWindowMaximize() {
		String str = properties.getProperty("window.maximize", "false");
		return Boolean.parseBoolean(str);
	}

	public void setWindowMaximize(boolean b) {
		properties.setProperty("window.maximize", Boolean.toString(b));
		save();
	}

	public Rectangle getWindowRect() {
		String x = properties.getProperty("window.x", "-1");
		String y = properties.getProperty("window.y", "-1");
		String width = properties.getProperty("window.width", "-1");
		String height = properties.getProperty("window.height", "-1");

		Rectangle rect = new Rectangle(
				Integer.parseInt(x), 
				Integer.parseInt(y),
				Integer.parseInt(width),
				Integer.parseInt(height)
				);

		return rect;
	}

	public void setWindowRect(Rectangle rect) {
		properties.setProperty("window.x", Integer.toString((int)rect.getX()));
		properties.setProperty("window.y", Integer.toString((int)rect.getY()));
		properties.setProperty("window.width", Integer.toString((int)rect.getWidth()));
		properties.setProperty("window.height", Integer.toString((int)rect.getHeight()));
		save();
	}

	public int getPianoRollViewHeightScaleProperty() {
		String s = properties.getProperty("view.pianoRoll.heightScale", "1");
		int index = Integer.parseInt(s);
		if ( (index < 0) || (index >= PianoRollView.NOTE_HEIGHT_TABLE.length) ) {
			index = 1;
		}

		return index;
	}

	public void setPianoRollViewHeightScaleProperty(int index) {
		properties.setProperty("view.pianoRoll.heightScale", ""+index);
		save();
	}

	public boolean getEnableClickPlay() {
		String str = properties.getProperty("function.enable_click_play", "true");
		return Boolean.parseBoolean(str);
	}

	public void setEnableClickPlay(boolean b) {
		properties.setProperty("function.enable_click_play", Boolean.toString(b));
		save();
	}
}
