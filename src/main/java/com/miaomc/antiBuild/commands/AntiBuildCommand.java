package com.miaomc.antiBuild.commands;

import com.miaomc.antiBuild.AntiBuild;
import com.miaomc.antiBuild.data.DataManager;
import com.miaomc.antiBuild.data.ProtectedArea;
import com.miaomc.antiBuild.data.ProtectedWorld;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class AntiBuildCommand implements CommandExecutor, TabCompleter {
    private final AntiBuild plugin;
    private final DataManager dataManager;

    public AntiBuildCommand(AntiBuild plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miaomc.antibuild.admin")) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "area" -> handleArea(sender, args);
            case "world" -> handleWorld(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize("&c用法: /antibuild create <area|world> <name>"));
            return true;
        }

        String type = args[1].toLowerCase();
        String name = args[2];

        switch (type) {
            case "area":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colorize("&c只有玩家可以创建区域！"));
                    return true;
                }
                dataManager.createArea(name, player.getWorld().getName());
                String areaMessage = plugin.getConfig().getString("messages.area-created", "&a区域 &e{name} &a创建成功！");
                sender.sendMessage(colorize(areaMessage.replace("{name}", name)));
                dataManager.saveData();
                break;
            case "world":
                dataManager.createWorld(name);
                String worldMessage = plugin.getConfig().getString("messages.world-created", "&a世界保护 &e{name} &a创建成功！");
                sender.sendMessage(colorize(worldMessage.replace("{name}", name)));
                dataManager.saveData();
                break;
            default:
                sender.sendMessage(colorize("&c用法: /antibuild create <area|world> <name>"));
                break;
        }
        return true;
    }

    private boolean handleArea(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize("&c用法: /antibuild area <name> <seta|setb|anti>"));
            return true;
        }

        String areaName = args[1];
        String action = args[2].toLowerCase();

        if (!dataManager.hasArea(areaName)) {
            String notFoundMessage = plugin.getConfig().getString("messages.area-not-found", "&c区域 &e{name} &c不存在！");
            sender.sendMessage(colorize(notFoundMessage.replace("{name}", areaName)));
            return true;
        }

        ProtectedArea area = dataManager.getArea(areaName);

        switch (action) {
            case "seta":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colorize("&c只有玩家可以设置点！"));
                    return true;
                }
                area.setPointA(player.getLocation());
                String pointAMessage = plugin.getConfig().getString("messages.point-a-set", "&a区域 &e{name} &a的点A已设置！");
                sender.sendMessage(colorize(pointAMessage.replace("{name}", areaName)));
                dataManager.saveData();
                break;
            case "setb":
                if (!(sender instanceof Player playerB)) {
                    sender.sendMessage(colorize("&c只有玩家可以设置点！"));
                    return true;
                }
                area.setPointB(playerB.getLocation());
                String pointBMessage = plugin.getConfig().getString("messages.point-b-set", "&a区域 &e{name} &a的点B已设置！");
                sender.sendMessage(colorize(pointBMessage.replace("{name}", areaName)));
                dataManager.saveData();
                break;
            case "anti":
                return handleAntiSettings(sender, args, area, "area");
            default:
                sender.sendMessage(colorize("&c用法: /antibuild area <name> <seta|setb|anti>"));
                break;
        }
        return true;
    }

    private boolean handleWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize("&c用法: /antibuild world <name> <anti>"));
            return true;
        }

        String worldName = args[1];
        String action = args[2].toLowerCase();

        if (!dataManager.hasWorld(worldName)) {
            String worldNotFoundMessage = plugin.getConfig().getString("messages.world-not-found", "&c世界保护 &e{name} &c不存在！");
            sender.sendMessage(colorize(worldNotFoundMessage.replace("{name}", worldName)));
            return true;
        }

        ProtectedWorld world = dataManager.getWorld(worldName);

        if ("anti".equals(action)) {
            return handleAntiSettings(sender, args, world, "world");
        } else {
            sender.sendMessage(colorize("&c用法: /antibuild world <name> anti <place|break|interaction|use> <true|false>"));
        }
        return true;
    }

    private boolean handleAntiSettings(CommandSender sender, String[] args, Object target, String type) {
        if (args.length < 5) {
            sender.sendMessage(colorize("&c用法: /antibuild " + type + " <name> anti <place|break|interaction|use> <true|false>"));
            return true;
        }

        String setting = args[3].toLowerCase();
        String value = args[4].toLowerCase();

        if (!value.equals("true") && !value.equals("false")) {
            sender.sendMessage(colorize("&c值必须是 true 或 false！"));
            return true;
        }

        boolean enabled = Boolean.parseBoolean(value);
        String targetName = target instanceof ProtectedArea ? ((ProtectedArea) target).getName() : ((ProtectedWorld) target).getName();

        switch (setting) {
            case "place":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiPlace(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiPlace(enabled);
                }
                break;
            case "break":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiBreak(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiBreak(enabled);
                }
                break;
            case "interaction":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiInteraction(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiInteraction(enabled);
                }
                break;
            case "use":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiUse(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiUse(enabled);
                }
                break;
            case "explosion":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiExplosion(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiExplosion(enabled);
                }
                break;
            case "fishing":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiFishing(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiFishing(enabled);
                }
                break;
            case "animal-interact":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiAnimalInteract(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiAnimalInteract(enabled);
                }
                break;
            case "throw":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiThrow(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiThrow(enabled);
                }
                break;
            case "shoot":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiShoot(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiShoot(enabled);
                }
                break;
            case "trample":
                if (target instanceof ProtectedArea) {
                    ((ProtectedArea) target).setAntiTrample(enabled);
                } else {
                    ((ProtectedWorld) target).setAntiTrample(enabled);
                }
                break;
            default:
                sender.sendMessage(colorize("&c无效的设置项: " + setting));
                return true;
        }

        String settingMessage = plugin.getConfig().getString("messages.setting-updated",
                "&a{type} &e{name} &a的 &e{action} &a设置已更新为 &e{value}&a！");
        sender.sendMessage(colorize(settingMessage
                .replace("{type}", type)
                .replace("{name}", targetName)
                .replace("{action}", setting)
                .replace("{value}", value)));

        dataManager.saveData();
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(colorize("&e=== AntiBuild 命令帮助 ==="));
        sender.sendMessage(colorize("&a/antibuild create area <name> &7- 创建保护区域"));
        sender.sendMessage(colorize("&a/antibuild create world <name> &7- 创建世界保护"));
        sender.sendMessage(colorize("&a/antibuild area <name> seta &7- 设置区域点A"));
        sender.sendMessage(colorize("&a/antibuild area <name> setb &7- 设置区域点B"));
        sender.sendMessage(colorize("&a/antibuild area <name> anti <place|break|interaction|use> <true|false>"));
        sender.sendMessage(colorize("&a/antibuild world <name> anti <place|break|interaction|use> <true|false>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "area", "world"));
        } else if (args.length == 2) {
            if ("create".equals(args[0])) {
                completions.addAll(Arrays.asList("area", "world"));
            } else if ("area".equals(args[0])) {
                completions.addAll(dataManager.getAreaNames());
            } else if ("world".equals(args[0])) {
                completions.addAll(dataManager.getWorldNames());
            }
        } else if (args.length == 3) {
            if ("area".equals(args[0])) {
                completions.addAll(Arrays.asList("seta", "setb", "anti"));
            } else if ("world".equals(args[0])) {
                completions.add("anti");
            }
        } else if (args.length == 4 && "anti".equals(args[2])) {
            completions.addAll(Arrays.asList("place", "break", "interaction", "use", "explosion", "fishing", "animal-interact", "throw", "shoot", "trample"));
        } else if (args.length == 5 && "anti".equals(args[2])) {
            completions.addAll(Arrays.asList("true", "false"));
        }

        return completions;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
