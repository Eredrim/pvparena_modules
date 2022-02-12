package net.slipcor.pvparena.modules.vaultsupport;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.events.PAPlayerClassChangeEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Field;
import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class VaultSupport extends ArenaModule implements Listener {

    private static Economy economy;
    private static Permission permission;
    private Map<String, Double> playerBetMap;
    private double pot;
    private Map<String, Double> list;

    public VaultSupport() {
        super("Vault");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String cmd) {
        return "bet".equalsIgnoreCase(cmd);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("bet");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"bet", "{Player}"});
        if (arena == null) {
            return result;
        }
        for (final String team : arena.getTeamNames()) {
            result.define(new String[]{"bet", team});
        }
        return result;
    }

    public boolean checkForBalance(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module "+module+" tries to check account "+sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(sender.getName())) {
            debug(sender, "Account not found: " + sender.getName());
            return false;
        }
        if (!economy.has(sender.getName(),
                amount)) {
            // no money, no entry!
            if (notify) {
                module.getArena().msg(sender, Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy
                        .format(amount)));
            } else {
                debug(sender, "Not enough cash!");
            }
            return false;
        }
        return true;
    }

    public boolean tryDeposit(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module "+module+" tries to deposit "+amount+" to "+sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(sender.getName())) {
            debug(sender, "Account not found: " + sender.getName());
            return false;
        }
        EconomyResponse res = economy.depositPlayer(sender.getName(), amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(Bukkit.getPlayer(sender.getName()), Language
                    .parse(MSG.MODULE_VAULT_YOUWON, economy.format(amount)));
            return true;
        }
        return false;
    }

    public static String tryFormat(ArenaModule module, int amount) {
        debug("module {} tries to format: {}", module, amount);
        if (economy == null) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }

    public boolean tryRefund(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module {} tries to refund {} to {}", module, amount, sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(sender.getName())) {
            debug(sender, "Account not found: {}", sender.getName());
            return false;
        }
        EconomyResponse res = economy.depositPlayer(sender.getName(), amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(Bukkit.getPlayer(sender.getName()), Language
                    .parse(MSG.MODULE_VAULT_REFUNDING, economy.format(amount)));
            return true;
        }
        return false;
    }

    public boolean tryWithdraw(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module {} tries to withdraw {} from {}", module, amount, sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(sender.getName())) {
            debug(sender, "Account not found: {}", sender.getName());
            return false;
        }
        if (!economy.has(sender.getName(),
                amount)) {
            // no money, no entry!
            if (notify) {
                module.getArena().msg(sender, Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy
                        .format(amount)));
            } else {
                debug(sender, "Not enough cash!");
            }
            return false;
        }
        EconomyResponse res = economy.withdrawPlayer(sender.getName(), amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(Bukkit.getPlayer(sender.getName()), Language
                    .parse(MSG.MODULE_VAULT_JOINPAY, economy.format(amount)));
            return true;
        }
        return false;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE) > 0) {
            if (economy != null) {
                if (!economy.hasAccount(player.getName())) {
                    debug(player, "Account not found: {}", player.getName());
                    throw new GameplayException("Account not found: " + player.getName());
                }
                if (!economy.has(player.getName(), this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE))) {
                    // no money, no entry!
                    throw new GameplayException(Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy
                            .format(this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE))));
                }
            }
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) { //TODO move to new parseCommand
            Language.parse(MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        final Player player = (Player) sender;

        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

        // /pa bet [name] [amount]
        if (ap.getArenaTeam() != null) {
            this.arena.msg(player, MSG.MODULE_VAULT_BETNOTYOURS);
            return;
        }

        if (economy == null) {
            return;
        }

        if ("bet".equalsIgnoreCase(args[0])) {

            final int maxTime = this.arena.getConfig().getInt(CFG.MODULES_VAULT_BETTIME);
            if (maxTime > 0 && maxTime > this.arena.getPlayedSeconds()) {
                this.arena.msg(player, MSG.ERROR_INVALID_VALUE, "2l8");
                return;
            }

            final Player p = Bukkit.getPlayer(args[1]);
            if (p != null) {
                ap = ArenaPlayer.fromPlayer(p);
            }
            if (p == null && this.arena.getTeam(args[1]) == null
                    && ap.getArenaTeam() == null) {
                this.arena.msg(player, MSG.MODULE_VAULT_BETOPTIONS);
                return;
            }

            final double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (final Exception e) {
                this.arena.msg(player, MSG.MODULE_VAULT_INVALIDAMOUNT, args[2]);
                return;
            }
            if (!economy.hasAccount(player.getName())) {
                debug(sender, "Account not found: " + player.getName());
                return;
            }
            if (!economy.has(player.getName(), amount)) {
                // no money, no entry!
                this.arena.msg(player, MSG.MODULE_VAULT_NOTENOUGH, economy.format(amount));
                return;
            }

            final double maxBet = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_MAXIMUMBET);

            if (amount < this.arena.getConfig().getDouble(CFG.MODULES_VAULT_MINIMUMBET)
                    || maxBet > 0.01 && amount > maxBet) {
                // wrong amount!
                this.arena.msg(player, MSG.ERROR_INVALID_VALUE, economy.format(amount));
                return;
            }

            economy.withdrawPlayer(player.getName(), amount);
            this.arena.msg(player, MSG.MODULE_VAULT_BETPLACED, args[1]);
            this.getPlayerBetMap().put(player.getName() + ':' + args[1], amount);
        }
    }

    @Override
    public boolean commitEnd(final ArenaTeam aTeam) {

        if (economy != null) {
            debug("eConomy set, parse bets");
            for (final String nKey : this.getPlayerBetMap().keySet()) {
                debug("bet: " + nKey);
                final String[] nSplit = nKey.split(":");

                if (this.arena.getTeam(nSplit[1]) == null
                        || "free".equals(this.arena.getTeam(nSplit[1]).getName())) {
                    continue;
                }

                if (nSplit[1].equalsIgnoreCase(aTeam.getName())) {
                    double teamFactor = this.arena.getConfig()
                            .getDouble(CFG.MODULES_VAULT_BETWINTEAMFACTOR)
                            * this.arena.getTeamNames().size();
                    if (teamFactor <= 0) {
                        teamFactor = 1;
                    }
                    teamFactor *= this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINFACTOR);

                    final double amount = this.getPlayerBetMap().get(nKey) * teamFactor;

                    if (!economy.hasAccount(nSplit[0])) {
                        debug("Account not found: {}", nSplit[0]);
                        return true;
                    }
                    debug("1 depositing {} to {}", amount, nSplit[0]);
                    if (amount > 0) {
                        economy.depositPlayer(nSplit[0], amount);
                        try {
                            this.arena.msg(Bukkit.getPlayer(nSplit[0]), Language
                                    .parse(MSG.MODULE_VAULT_YOUWON, economy.format(amount)));
                        } catch (final Exception e) {
                            // nothing
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    private Map<String, Double> getPermList() {
        if (this.list == null) {
            this.list = new HashMap<>();

            if (this.arena.getConfig().getYamlConfiguration().contains("modules.vault.permfactors")) {
                List<String> cs = this.arena.getConfig().getYamlConfiguration().
                        getStringList("modules.vault.permfactors");
                for (String node : cs) {
                    String[] split = node.split(":");
                    try {
                        this.list.put(split[0], Double.parseDouble(split[1]));
                    } catch (Exception e) {
                        PVPArena.getInstance().getLogger().warning(
                                "string '" + node + "' could not be read in node 'modules.vault.permfactors' in arena " + this.arena.getName());
                    }
                }
            } else {

                this.list.put("pa.vault.supervip", 3.0d);
                this.list.put("pa.vault.vip", 2.0d);

                List<String> stringList = new ArrayList<>();

                for (Map.Entry<String, Double> stringDoubleEntry : this.list.entrySet()) {
                    stringList.add(stringDoubleEntry.getKey() + ':' + stringDoubleEntry.getValue());
                }
                this.arena.getConfig().setManually("modules.vault.permfactors", stringList);
                this.arena.getConfig().save();
            }
        }
        return this.list;
    }

    /**
     * bettingPlayerName:betGoal => betAmount
     *
     */
    private Map<String, Double> getPlayerBetMap() {
        if (this.playerBetMap == null) {
            this.playerBetMap = new HashMap<>();
        }
        return this.playerBetMap;
    }

    @Override
    public void giveRewards(final Player player) {
        if (player == null) {
            return;
        }

        final int minPlayTime = this.arena.getConfig().getInt(CFG.MODULES_VAULT_MINPLAYTIME);

        if (minPlayTime > this.arena.getPlayedSeconds()) {
            debug("no rewards, game too short!");
            return;
        }

        final int minPlayers = this.arena.getConfig().getInt(CFG.MODULES_VAULT_MINPLAYERS);

        Field field = null;
        try {
            field = this.arena.getClass().getDeclaredField("startCount");
            field.setAccessible(true);
            if (minPlayers > field.getInt(this.arena)) {
                debug("no rewards, not enough players!");
                return;
            }
        } catch (final NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        debug(player, "giving rewards to player {}", player.getName());

        debug(player, "giving Vault rewards to Player {}", player);
        int winners = 0;
        for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            debug(arenaPlayer, "- checking fighter {}", arenaPlayer.getName());
            if (arenaPlayer.getStatus() != null && arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                debug(arenaPlayer, "-- added!");
                winners++;
            }
        }
        debug(player, "winners: " + winners);

        if (economy != null) {
            debug(player, "checking on bet amounts!");
            for (final String nKey : this.getPlayerBetMap().keySet()) {
                final String[] nSplit = nKey.split(":");

                if (nSplit[1].equalsIgnoreCase(player.getName())) {
                    double playerFactor = this.arena.getFighters().size()
                            * this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINPLAYERFACTOR);

                    if (playerFactor <= 0) {
                        playerFactor = 1;
                    }

                    playerFactor *= this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINFACTOR);

                    final double amount = this.getPlayerBetMap().get(nKey) * playerFactor;

                    debug("2 depositing {} to {}", amount, nSplit[0]);
                    if (amount > 0) {
                        economy.depositPlayer(nSplit[0], amount);
                        try {

                            ArenaModuleManager.announce(
                                    this.arena,
                                    Language.parse(MSG.NOTICE_PLAYERAWARDED,
                                            economy.format(amount)), "PRIZE");
                            this.arena.msg(Bukkit.getPlayer(nSplit[0]), Language
                                    .parse(MSG.MODULE_VAULT_YOUWON, economy.format(amount)));
                        } catch (final Exception e) {
                            // nothing
                        }
                    }
                }
            }

            if (this.arena.getConfig().getBoolean(CFG.MODULES_VAULT_WINPOT)) {
                debug(player, "calculating win pot!");
                double amount = winners > 0 ? this.pot / winners : 0;


                double factor = 1.0d;
                for (final String node : this.getPermList().keySet()) {
                    if (player.hasPermission(node)) {
                        factor = Math.max(factor, this.getPermList().get(node));
                    }
                }

                amount *= factor;

                debug("3 depositing {} to {}", amount, player.getName());
                if (amount > 0) {
                    economy.depositPlayer(player.getName(), amount);
                    this.arena.msg(player, MSG.NOTICE_AWARDED, economy.format(amount));
                }
            } else if (this.arena.getConfig().getInt(CFG.MODULES_VAULT_WINREWARD, 0) > 0) {

                double amount = this.arena.getConfig().getInt(CFG.MODULES_VAULT_WINREWARD, 0);
                debug(player, "calculating win reward: {}", amount);


                double factor;

                try {
                    factor = Math.pow(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_WINREWARDPLAYERFACTOR)
                            , field.getInt(this.arena));
                } catch (final Exception e) {
                    PVPArena.getInstance().getLogger().warning("Failed to get playedPlayers, using winners!");
                    factor = Math.pow(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_WINREWARDPLAYERFACTOR)
                            , winners);
                }

                for (final String node : this.getPermList().keySet()) {
                    if (player.hasPermission(node)) {
                        factor = Math.max(factor, this.getPermList().get(node));
                        debug(player, "has perm '{}'; factor set to {}", node, factor);
                    }
                }

                amount *= factor;

                debug("4 depositing {} to {}", amount, player.getName());
                if (amount > 0) {
                    economy.depositPlayer(player.getName(), amount);
                    this.arena.msg(player, MSG.NOTICE_AWARDED, economy.format(amount));
                }
            }
        }
    }

    private void killreward(final Entity damager) {
        Player player = null;
        if (damager instanceof Player) {
            player = (Player) damager;
        }
        if (player == null) {
            return;
        }
        double amount = this.arena.getConfig()
                .getDouble(CFG.MODULES_VAULT_KILLREWARD);

        if (amount < 0.01) {
            return;
        }

        if (!economy.hasAccount(player.getName())) {
            debug(player, "Account not found: {}", player.getName());
            return;
        }

        double factor = 1.0d;
        for (final String node : this.getPermList().keySet()) {
            if (player.hasPermission(node)) {
                factor = Math.max(factor, this.getPermList().get(node));
            }
        }

        amount *= factor;
        debug("6 depositing {} to {}", amount, player.getName());

        if (amount > 0) {
            economy.depositPlayer(player.getName(), amount);
            try {
                this.arena.msg(Bukkit.getPlayer(player.getName()), Language
                        .parse(MSG.MODULE_VAULT_YOUWON, economy.format(amount)));
            } catch (final Exception e) {
                // nothing
            }
        }
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("entryfee: "
                + StringParser.colorVar(this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE))
                + " || reward: "
                + StringParser.colorVar(this.arena.getConfig().getInt(CFG.MODULES_VAULT_WINREWARD))
                + " || rewardPlayerFactor: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_WINREWARDPLAYERFACTOR))
                + " || killreward: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_KILLREWARD))
                + " || winFactor: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_WINFACTOR)));

        player.sendMessage("minbet: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_MINIMUMBET))
                + " || maxbet: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_MAXIMUMBET))
                + " || minplayers: "
                + StringParser.colorVar(this.arena.getConfig().getInt(CFG.MODULES_VAULT_MINPLAYERS))
                + " || betWinFactor: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINFACTOR)));

        player.sendMessage("betTeamWinFactor: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINTEAMFACTOR))
                + " || betPlayerWinFactor: "
                + StringParser.colorVar(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINPLAYERFACTOR)));

        player.sendMessage(StringParser.colorVar(
                "bet pot", this.arena.getConfig().getBoolean(
                        CFG.MODULES_VAULT_BETPOT))
                + " || "
                + StringParser.colorVar(
                "win pot", this.arena.getConfig().getBoolean(
                        CFG.MODULES_VAULT_WINPOT)));
    }

    @Override
    public void onThisLoad() {
        if (economy == null && Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            this.setupEconomy();
            this.setupPermission();

            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        final int entryfee = this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE, 0);
        if (entryfee > 0) {
            if (economy != null) {
                economy.withdrawPlayer(player.getName(), entryfee);
                this.arena.msg(player, MSG.MODULE_VAULT_JOINPAY, economy.format(entryfee));
                this.pot += entryfee;
            }
        }
    }

    @Override
    public void parsePlayerDeath(final Player p,
                                 final EntityDamageEvent cause) {
        this.killreward(ArenaPlayer.getLastDamagingPlayer(cause));
    }

    void pay(final Set<String> result) {
        if (result == null || result.size() == this.arena.getTeamNames().size()) {
            return;
        }
        debug("Paying winners: {}", StringParser.joinSet(result, ", "));

        if (economy == null) {
            return;
        }

        double pot = 0;
        double winpot = 0;

        for (final String s : this.getPlayerBetMap().keySet()) {
            final String[] nSplit = s.split(":");

            pot += this.getPlayerBetMap().get(s);

            if (result.contains(nSplit)) {
                winpot += this.getPlayerBetMap().get(s);
            }
        }

        for (final String nKey : this.getPlayerBetMap().keySet()) {
            final String[] nSplit = nKey.split(":");
            final ArenaTeam team = this.arena.getTeam(nSplit[1]);
            if (team == null || "free".equals(team.getName())) {
                if (Bukkit.getPlayerExact(nSplit[1]) == null) {
                    continue;
                }
            }

            if (result.contains(nSplit[1])) {
                double amount = 0;

                if (this.arena.getConfig().getBoolean(CFG.MODULES_VAULT_BETPOT)) {
                    if (winpot > 0) {
                        amount = pot * this.getPlayerBetMap().get(nKey) / winpot;
                    }
                } else {
                    double teamFactor = this.arena.getConfig()
                            .getDouble(CFG.MODULES_VAULT_BETWINTEAMFACTOR)
                            * this.arena.getTeamNames().size();
                    if (teamFactor <= 0) {
                        teamFactor = 1;
                    }
                    teamFactor *= this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BETWINFACTOR);
                    amount = this.getPlayerBetMap().get(nKey) * teamFactor;
                }

                if (!economy.hasAccount(nSplit[0])) {
                    debug("Account not found: {}", nSplit[0]);
                    continue;
                }

                final Player player = Bukkit.getPlayer(nSplit[0]);

                if (player == null) {
                    debug("Player is null!");
                } else {
                    double factor = 1.0d;
                    for (final String node : this.getPermList().keySet()) {
                        if (player.hasPermission(node)) {
                            factor = Math.max(factor, this.getPermList().get(node));
                        }
                    }

                    amount *= factor;
                }

                debug("7 depositing {} to {}", amount, nSplit[0]);
                if (amount > 0) {
                    economy.depositPlayer(nSplit[0], amount);
                    try {
                        this.arena.msg(Bukkit.getPlayer(nSplit[0]), Language
                                .parse(MSG.MODULE_VAULT_YOUWON, economy.format(amount)));
                    } catch (final Exception e) {
                        // nothing
                    }
                }
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getPlayerBetMap().clear();
        this.pot = 0;
    }

    @Override
    public void resetPlayer(final Player player, final boolean soft, final boolean force) {
        if (player == null) {
            return;
        }
        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        if (ap == null) {
            return;
        }
        if (ap.getStatus() == null || force) {
            return;
        }
        if (ap.getStatus() == PlayerStatus.LOUNGE ||
                ap.getStatus() == PlayerStatus.READY) {
            final int entryfee = this.arena.getConfig().getInt(CFG.MODULES_VAULT_ENTRYFEE);
            if (entryfee < 1) {
                return;
            }
            this.arena.msg(player, MSG.MODULE_VAULT_REFUNDING, economy.format(entryfee));
            if (!economy.hasAccount(player.getName())) {
                debug(player, "Account not found: {}", player.getName());
                return;
            }
            debug("8 depositing {} to {}", entryfee, player.getName());
            economy.depositPlayer(player.getName(), entryfee);
            this.pot -= entryfee;

        }
    }

    private boolean setupEconomy() {
        final RegisteredServiceProvider<Economy> economyProvider = Bukkit
                .getServicesManager().getRegistration(
                        Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return economy != null;
    }

    private boolean setupPermission() {
        final RegisteredServiceProvider<Permission> permProvider = Bukkit
                .getServicesManager().getRegistration(
                        Permission.class);
        if (permProvider != null) {
            permission = permProvider.getProvider();
        }

        return permission != null;
    }

    public void timedEnd(final HashSet<String> result) {
        this.pay(result);
    }

    @EventHandler
    public void onClassChange(final PAPlayerClassChangeEvent event) {
        if (event.getArena() != null && event.getArena().equals(this.arena)) {

            final String autoClass = this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS);

            if (event.getArenaClass() == null || !event.getArenaClass().getName().equals(autoClass)) {
                return; // class will be removed OR no autoClass OR no>T< autoClass
            }

            String group = null;

            try {
                group = permission.getPrimaryGroup(event.getPlayer());
            } catch (final Exception e) {

            }
            final ArenaClass aClass = this.arena.getClass("autoClass_" + group);
            if (aClass != null) {
                event.setArenaClass(aClass);
            }
        }
    }

    @EventHandler
    public void onGoalScore(final PAGoalEvent event) {


        if (event.getArena().equals(this.arena)) {
            debug("it's us!");
            final String[] contents = event.getContents();
            /*
			* content.length == 1
			* * content[0] = "" => end!
			* 
			* content[X].contains(playerDeath) => "playerDeath:playerName"
			* content[X].contains(playerKill) => "playerKill:playerKiller:playerKilled"
			* content[X].contains(trigger) => "trigger:playerName" triggered a score
			* content[X].equals(tank) => player is tank
			* content[X].equals(infected) => player is infected
			* content[X].equals(doesRespawn) => player will respawn
			* content[X].contains(score) => "score:player:team:value"
			*
			*/

            String lastTrigger = "";
            for (String node : contents) {
                node = node.toLowerCase();
                if (node.contains("trigger")) {
                    lastTrigger = node.substring(8);
                    this.newReward(lastTrigger, "TRIGGER");
                }

                if (node.contains("playerDeath")) {
                    this.newReward(node.substring(12), "DEATH");
                }

                if (node.contains("playerKill")) {
                    final String[] val = node.split(":");
                    if (!val[1].equals(val[2])) {
                        this.newReward(val[1], "KILL");
                    }
                }

                if (node.contains("score")) {
                    final String[] val = node.split(":");
                    this.newReward(val[1], "SCORE", Integer.parseInt(val[3]));
                }

                if (node != null && node.isEmpty() && lastTrigger != null && !lastTrigger.isEmpty()) {
                    this.newReward(lastTrigger, "WIN");
                }
            }

        }
    }

    private void newReward(final String playerName, final String rewardType) {
        if (playerName == null || playerName.length() < 1) {
            PVPArena.getInstance().getLogger().warning("winner is empty string in " + this.arena.getName());
            return;
        }
        debug("new Reward: {} -> {}", playerName, rewardType);
        this.newReward(playerName, rewardType, 1);
    }

    private void newReward(final String playerName, final String rewardType, final int amount) {
        if (playerName == null || playerName.length() < 1) {
            PVPArena.getInstance().getLogger().warning("winner is empty string in " + this.arena.getName());
            return;
        }
        debug("new Reward: {}x {} -> {}", amount, playerName, rewardType);
        try {

            double value = this.arena.getConfig().getDouble(
                    CFG.valueOf("MODULES_VAULT_REWARD_" + rewardType), 0.0d);

            final double maybevalue = this.arena.getConfig().getDouble(
                    CFG.valueOf("MODULES_VAULT_REWARD_" + rewardType), -1.0d);

            if (maybevalue < 0) {
                PVPArena.getInstance().getLogger().warning("config value is not set: " + CFG.valueOf("MODULES_VAULT_REWARD_" + rewardType).getNode());
            }
            final Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                double factor = 1.0d;
                for (final String node : this.getPermList().keySet()) {
                    if (player.hasPermission(node)) {
                        factor = Math.max(factor, this.getPermList().get(node));
                    }
                }

                value *= factor;
            }

            debug("9 depositing {} to {}", value, playerName);
            if (value > 0) {
                economy.depositPlayer(playerName, value);
                try {

                    ArenaModuleManager.announce(
                            this.arena,
                            Language.parse(MSG.NOTICE_PLAYERAWARDED,
                                    economy.format(value)), "PRIZE");
                    this.arena.msg(player, Language
                            .parse(MSG.MODULE_VAULT_YOUWON, economy.format(value)));
                } catch (final Exception e) {
                    // nothing
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
