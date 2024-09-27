package be.speech;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import edu.cmu.sphinx.linguist.g2p.Path;

public class Main {
	
	private static boolean active = true;

	private static Thread activate = new Thread() {
		@Override
		public void run() {
			commit();
		}
	};

	public static List<Command> commands = new ArrayList<Command>();

	static {
		commands.add(new Command("open edge", "cmd.exe /c start msedge www.google.com"));
		commands.add(new Command("close edge", "cmd.exe /c TASKKILL /IM msedge.exe"));
	}

	public static String getJarFolder() {
		String name = Main.class.getName().replace('.', '/');
		String s = Main.class.getResource("/" + name + ".class").toString();
		s = s.replace('/', File.separatorChar);
		if (s.indexOf(".jar") != -1) {
			s = s.substring(0, s.indexOf(".jar") + 4);
		} else {
			s = s.substring(0, s.indexOf("target"));
		}
		if (s.lastIndexOf(':') != -1) {
			s = s.substring(s.lastIndexOf(':') - 1);
		}
		return s.substring(0, s.lastIndexOf(File.separatorChar) + 1).replace("%20", " ").replace("\\", "/");
	}
	
	public static void commit() {
		boolean flag = true;
		readVoiceCommands();
		try {
			createDictionary();
			createGrammar();
		} catch(Exception e) {
			e.printStackTrace();
			flag=false;
		}
		if(flag) {
			say("Commands are loaded!");
			listen();
		}else {
			say("A problem has occurred when loading commands!");
		}
	}

	public static void say(String word) {
		System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
		VoiceManager voiceManager = VoiceManager.getInstance();
		// Voice[] voices = voiceManager.getVoices();
		// for (int i = 0; i < voices.length; i++) {
		//     System.out.println(" " + voices[i].getName() + " (" + voices[i].getDomain() + " domain)");
		// }
		Voice voice = voiceManager.getVoice("kevin16");
		// voice.setPitch(1.75f);
		// voice.setPitchShift(0.75f);
		// mutace
		// voice.setPitchRange(10.1f);
		// "business", "casual", "robotic", "breathy"
		// voice.setStyle("casual");
		voice.allocate();
		voice.speak(word);
		voice.deallocate();
	}

	public static void listen() {
		// go to http://www.speech.cs.cmu.edu/tools/lmtool-new.html for creating
		// dictionary and languagemodel
		Configuration config = new Configuration();
		config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		config.setDictionaryPath("file:///" + getJarFolder() + "commands.dict");
		//config.setLanguageModelPath("file:///" + getJarFolder() + "commands.lm");
		config.setGrammarPath("file:///"+getJarFolder());
		config.setGrammarName("commands.grxml");
		config.setUseGrammar(true);
		try {
			LiveSpeechRecognizer speech = new LiveSpeechRecognizer(config);
			speech.startRecognition(true);
			SpeechResult speechResult = null;
			while (active && (speechResult = speech.getResult()) != null) {
				String voiceCommand = speechResult.getHypothesis();
				if(!"".equals(voiceCommand)) {
					System.out.println("Voice Command is " + voiceCommand);
					for (Command c : commands) {
						if (voiceCommand.equalsIgnoreCase(c.getName())) {
							say("Voice commands found!");
							Runtime.getRuntime().exec(c.getProgram());
							break;
						}
					}
				}
			}
			speech.stopRecognition();
			speech.closeRecognitionLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readVoiceCommands() {
		File file = new File(getJarFolder() + "commands.txt");
		try {
			if (file.exists()) {
				commands.clear();
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] values = line.split("\t");
					commands.add(new Command(values[0], values[1]));
				}
				reader.close();
			} else {
				file.createNewFile();
				if (commands.size() != 0) {
					writeVoiceCommands();
				}
			}
		} catch (IOException ioe) {
		}
	}

	public static void writeVoiceCommands() throws IOException {
		File file = new File(getJarFolder() + "commands.txt");
		if (!file.exists()) {
			file.createNewFile();
		}
		String str = "";
		for (Command c : commands) {
			str += c.getName() + "\t" + c.getProgram() + "\n";
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(str);
		writer.close();
	}

	public static void createDictionary() throws Exception {
		File file = new File(getJarFolder() + "commands.dict");
		if (!file.exists()) {
			file.createNewFile();
		}
		String str = "";
		Set<String> words = new HashSet<String>();
		for (Command c : commands) {
			String[] parts = c.getName().split(" ");
			for (String p : parts) {
				words.add(p);
			}
		}
		List<String> wordsSorted = new ArrayList<String>(words);
		Collections.sort(wordsSorted);
		URL url = Main.class.getResource("model.fst.ser");
		G2PConverter converter = new G2PConverter(url);
		InputStream dict = Main.class.getResourceAsStream("cmudict-5prealpha.dict");
		BufferedReader br = new BufferedReader(new InputStreamReader(dict, StandardCharsets.UTF_8));
		String dictionary = br.lines().collect(Collectors.joining("\n"));
		br.close();
		dict.close();
		for (String word : wordsSorted) {
			if (dictionary.contains("\n" + word + " ")) {
				int start = dictionary.indexOf("\n" + word + " ") + 1;
				int end = dictionary.indexOf("\n", start) + 1;
				str += dictionary.substring(start, end);
				for (int count = 2; dictionary.contains("\n" + word + "(" + count + ") "); count++) {
					int s = dictionary.indexOf("\n" + word + "(" + count + ") ") + 1;
					int e = dictionary.indexOf("\n", s) + 1;
					str += dictionary.substring(s, e);
				}
			} else {
				ArrayList<Path> list = converter.phoneticize(word, 1);
				int k = 1;
				for (Path item : list) {
					str += word + ((k > 1) ? "(" + k + ")" : "") + " ";
					List<String> path = item.getPath();
					int j = 1, size = path.size();
					for (String i : path) {
						str += i;
						if (j < size)
							str += " ";
						j++;
					}
					str += "\n";
					k++;
				}
			}
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(str);
		writer.close();
	}

	public static void createGrammar() throws IOException {
		File file = new File(getJarFolder() + "commands.grxml");
		if (!file.exists()) {
			file.createNewFile();
		}
		Set<String> words = new HashSet<String>();
		for (Command c : commands) {
			String[] parts = c.getName().split(" ");
			for (String p : parts) {
				words.add(p);
			}
		}
		List<String> wordsSorted = new ArrayList<String>(words);
		Collections.sort(wordsSorted);
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n";
		str+= "<!DOCTYPE grammar PUBLIC \"-//W3C//DTD GRAMMAR 1.0//EN\" \"http://www.w3.org/TR/speech-grammar/grammar.dtd\">\r\n";
		str+= "<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xml:lang=\"en\" xsi:schemaLocation=\"http://www.w3.org/TR/speech-grammar/grammar.xsd\" version=\"1.0\" mode=\"voice\" root=\"commands\">\r\n";
		str+= "<rule id=\"commands\" scope=\"public\">\r\n";
		str+= " <item repeat=\"1-\">\r\n";
		str+= "  <one-of>\r\n";
		for (String word : wordsSorted) {
			str+="   <item> "+word+" </item>\n";
		}
		str+="  </one-of>\r\n";
		str+=" </item>\r\n";
		str+="</rule>\r\n";
		str+="</grammar>";
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(str);
		writer.close();
	}

	public static void main(String[] args) throws Exception {
		if (SystemTray.isSupported()) {
			SystemTray st = SystemTray.getSystemTray();
			Image image = ImageIO.read(Main.class.getResourceAsStream("trayicon.png"));
			TrayIcon icon = new TrayIcon(image, "Programlauncher");
			PopupMenu menu = new PopupMenu();
			MenuItem item1 = new MenuItem("Open");
			item1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					readVoiceCommands();
					JFrame frame = new JFrame();
					// Set layout manager
			        frame.setLayout(new BorderLayout());
					// create the model
					CommandTableModel model = new CommandTableModel();
					// create the table
					JTable table = new JTable(model);
					// add the table to the frame
					frame.add(new JScrollPane(table), BorderLayout.CENTER);
			        // Create a panel for buttons
			        JPanel buttonPanel = new JPanel();
			        // 1 row, 2 columns with spacing
			        buttonPanel.setLayout(new GridLayout(1, 3, 10, 10));
					JButton add = new JButton("Add Command");
					add.addActionListener(new ActionListener() {
						@Override
			            public void actionPerformed(ActionEvent e) {
			                commands.add(new Command("new command",""));
			                ((CommandTableModel)table.getModel()).fireTableDataChanged();
			                frame.repaint();
			            }
			        });
					buttonPanel.add(add);
					JButton remove = new JButton("Remove Command");
					remove.addActionListener(new ActionListener() {
						@Override
			            public void actionPerformed(ActionEvent e) {
			                int i = table.getSelectedRow();
			                commands.remove(i);
			                ((CommandTableModel)table.getModel()).fireTableDataChanged();
			                frame.repaint();
			            }
			        });
					buttonPanel.add(remove);
					JButton commit = new JButton("Commit");
					commit.addActionListener(new ActionListener() {
						@Override
			            public void actionPerformed(ActionEvent e) {
							try {
								writeVoiceCommands();
							} catch(IOException ioe) {
								ioe.printStackTrace();
							} finally {
								active = false;
								while(!Thread.State.TERMINATED.equals(activate.getState())) {
									// do nothing ...
								}
								active = true;
								activate = new Thread() {
									@Override
									public void run() {
										commit();
									}
								};
								activate.start();
							}
			            }
			        });
					buttonPanel.add(commit);
					frame.add(buttonPanel, BorderLayout.SOUTH);
					frame.setTitle("Programlauncher");
					try {
						Image image = ImageIO.read(Main.class.getResourceAsStream("trayicon.png"));
						frame.setIconImage(image);
					} catch (IOException ioe) {
					}
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					frame.pack();
					frame.setVisible(true);
				}
			});
			menu.add(item1);
			MenuItem item2 = new MenuItem("Exit");
			item2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					active=false;
					while(!Thread.State.TERMINATED.equals(activate.getState())) {
						// do nothing ...
					}
					System.exit(0);
				}
			});
			menu.add(item2);
			icon.setPopupMenu(menu);
			st.add(icon);
		}
		activate.start();
	}

}
