package net.gamersconnection.forge;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent.Save;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VER)
public class OverviewerConfigGeneratorMod {
	static Logger log = LogManager.getLogger(ModInfo.ID);

	private Map<String, Map<String, String>> worlds = new TreeMap<String, Map<String, String>>();

	private static Boolean enableSmoothLighting;
	private static Boolean enableCaveRender;
	private static Boolean enableNightRender;
	private static File worldFolder;
	private static String worldName;

	// The instance of your mod that Forge uses.
	@Instance(ModInfo.ID)
	public static OverviewerConfigGeneratorMod instance;

	/**
	 * PreInit hook.
	 * 
	 * @param event
	 */
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(
				event.getSuggestedConfigurationFile());

		config.load();

		enableSmoothLighting = config.get(Configuration.CATEGORY_GENERAL,
				"enableSmoothLighting", true).getBoolean();
		enableCaveRender = config.get(Configuration.CATEGORY_GENERAL,
				"enableCaveRender", true).getBoolean();
		enableNightRender = config.get(Configuration.CATEGORY_GENERAL,
				"enableNightRender", false).getBoolean();

		config.save();
	}

	/**
	 * PostInit hook.
	 * 
	 * @param event
	 */
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		log.info("Registering World.Save handler");
		MinecraftForge.EVENT_BUS.register(new WorldSaveEventHandler());
	}

	/**
	 * @author Tom
	 *
	 *         Event handler for world save events.
	 */
	public class WorldSaveEventHandler {
		/**
		 * On save we will write our our world config for Overviewer.
		 * 
		 * @param event
		 */
		@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
		public void onEvent(Save event) {
			worldFolder = event.world.getSaveHandler().getWorldDirectory();
			worldName = event.world.getWorldInfo().getWorldName();
			processWorld(event.world.provider);
			
			try {
				writeConfig();
			} catch (IOException e) {
				log.error("Unable to write overviewer configuration");
			}
		}
	}

	/**
	 * Process the provided world.provider into our world map.
	 * 
	 * @param provider
	 */
	private void processWorld(WorldProvider provider) {
		// Get the world dimension information
		short dim = (short) provider.dimensionId;
		String name = provider.getDimensionName();
		String saveFolder = provider.getSaveFolder();

		Map<String, String> world = new WorldHashMap<String, String>();
		world.put("world", worldName);
		world.put("title", name);
		switch (dim) {
		case 0: // overworld
			setOverworld(name);
			break;
		case -1: // nether
			if (enableSmoothLighting) {
				world.put("rendermode", "nether_lighting_smooth");
			} else {
				world.put("rendermode", "nether_lighting");
			}
			world.put("dimension", "nether");
			addWorldConfig("3-nether", world);
			break;
		case 1: // end
			world.put("dimension", "end");
			addWorldConfig("4-end", world);
			break;
		default:
			if (enableSmoothLighting) {
				world.put("rendermode", "smooth_lighting");
			} else {
				world.put("rendermode", "lighting");
			}
			world.put("dimension", saveFolder);
			addWorldConfig(saveFolder, world);
			break;
		}

	}

	/**
	 * Helper method to add a world config to our world map. Only adds if the
	 * world is not already present.
	 * 
	 * @param name
	 * @param worldConfig
	 */
	private void addWorldConfig(String name, Map<String, String> worldConfig) {
		if (!worlds.containsKey(name)) {
			worlds.put(name, worldConfig);
		}
	}

	/**
	 * Helper method for overworld since it can be multiple maps depending on
	 * configuration.
	 * 
	 * @param name
	 */
	private void setOverworld(String name) {
		/*
		 * Our default overworld entry
		 */
		Map<String, String> world = new WorldHashMap<String, String>();
		world.put("world", worldName);
		world.put("title", name);
		world.put("dimension", "overworld");
		if (enableSmoothLighting) {
			world.put("rendermode", "smooth_lighting");
		} else {
			world.put("rendermode", "lighting");
		}
		addWorldConfig("0-" + name, world);

		/*
		 * if we are requested to render caves, add a new entry
		 */
		if (enableCaveRender) {
			String title = name + " Caves";
			Map<String, String> cave = new WorldHashMap<String, String>();
			cave.put("world", worldName);
			cave.put("title", title);
			cave.put("rendermode", "cave");
			cave.put("dimension", "overworld");
			addWorldConfig("1-" + name, cave);
		}

		/*
		 * if we are requested to render night, add a new entry
		 */
		if (enableNightRender) {
			String title = name + " Night";
			Map<String, String> night = new WorldHashMap<String, String>();
			night.put("world", worldName);
			night.put("title", title);
			if (enableSmoothLighting) {
				night.put("rendermode", "night_smooth");
			} else {
				night.put("rendermode", "night");
			}
			night.put("dimension", "overworld");
			addWorldConfig("2-" + name, night);
		}
	}

	/**
	 * Write out the worlds to a file compatible with Overviewer configuration.
	 * 
	 * @throws IOException
	 */
	private void writeConfig() throws IOException {
		File file = new File("overviewer-worlds.conf");

		PrintWriter out = new PrintWriter(file);

		// write out our world configuration
		out.println(String
				.format("worlds['%s'] = '%s'", worldName, worldFolder.getAbsolutePath()));
		out.println();
		for (String world : worlds.keySet()) {
			out.println(String.format("renders['%s'] = %s", world,
					worlds.get(world).toString()));
		}

		out.close();
	}
}
