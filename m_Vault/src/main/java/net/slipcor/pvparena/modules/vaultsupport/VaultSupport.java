package net.slipcor.pvparena.modules.vaultsupport;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.exceptions.GameplayRuntimeException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

public class VaultSupport extends ArenaModule {

    private static Economy economy;
    private final Map<UUID, SimpleEntry<String, Double>> playerBetMap = new HashMap<>();
    private boolean betEnabled = false;
    private double pot;
    private int endPlayerNumber;
    private int winnerNumber;
    private VaultListener activeListener;

    public VaultSupport() {
        super("Vault");
        if (economy == null && Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            final RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        if (economy == null) {
            throw new GameplayRuntimeException("Vault plugin is not found! So, Vault module will not be enabled in arenas.");
        }
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
        if(this.betEnabled) {
            return Collections.singletonList("bet");
        }
        return Collections.emptyList();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        CommandTree<String> result = new CommandTree<>(null);
        if(this.arena.isFreeForAll()) {
            result.define(new String[]{"{Player}"});
        } else {
            this.arena.getTeamNames().forEach(team -> result.define(new String[]{team}));
        }

        return result;
    }

    @Override
    public void configParse(YamlConfiguration config) {
        this.betEnabled = this.arena.getConfig().getBoolean(CFG.MODULES_VAULT_BET_ENABLED);
    }

    public boolean checkForBalance(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module "+module+" tries to check account "+sender.getName());
        Player player = Bukkit.getPlayer(sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(player)) {
            debug(sender, "[Vault] Account not found: " + sender.getName());
            return false;
        }
        if (!economy.has(player, amount)) {
            // no money, no entry!
            if (notify) {
                module.getArena().msg(sender, Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy.format(amount)));
            } else {
                debug(sender, "[Vault] Not enough cash!");
            }
            return false;
        }
        return true;
    }

    public boolean tryDeposit(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module "+module+" tries to deposit "+amount+" to "+sender.getName());
        Player player = Bukkit.getPlayer(sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(player)) {
            debug(sender, "[Vault] Account not found: " + sender.getName());
            return false;
        }
        EconomyResponse res = economy.depositPlayer(player, amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(player, Language.parse(MSG.MODULE_VAULT_YOUEARNED, economy.format(amount)));
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
        Player player = Bukkit.getPlayer(sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(player)) {
            debug(sender, "[Vault] Account not found: {}", sender.getName());
            return false;
        }
        EconomyResponse res = economy.depositPlayer(player, amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(player, Language.parse(MSG.MODULE_VAULT_REFUNDING, economy.format(amount)));
            return true;
        }
        return false;
    }

    public boolean tryWithdraw(ArenaModule module, CommandSender sender, int amount, boolean notify) {
        debug(sender, "module {} tries to withdraw {} from {}", module, amount, sender.getName());
        Player player = Bukkit.getPlayer(sender.getName());
        if (economy == null) {
            return false;
        }
        if (!economy.hasAccount(player)) {
            debug(sender, "[Vault] Account not found: {}", sender.getName());
            return false;
        }
        if (!economy.has(player, amount)) {
            // no money, no entry!
            if (notify) {
                module.getArena().msg(sender, Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy
                        .format(amount)));
            } else {
                debug(sender, "[Vault] Not enough cash!");
            }
            return false;
        }
        EconomyResponse res = economy.withdrawPlayer(player, amount);
        if (res.transactionSuccess() && notify) {
            this.arena.msg(player, Language.parse(MSG.MODULE_VAULT_JOINPAY, economy.format(amount)));
            return true;
        }
        return false;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.arena.getConfig().getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE) > 0) {
            if (economy != null) {
                if (!economy.hasAccount(player)) {
                    debug(player, "[Vault] Account not found");
                    throw new GameplayException(Language.parse(MSG.MODULE_VAULT_NOACCOUNT));
                }
                if (!economy.has(player, this.arena.getConfig().getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE))) {
                    // no money, no entry!
                    throw new GameplayException(Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy
                            .format(this.arena.getConfig().getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE))));
                }
            }
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            Language.parse(MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{1, 2, 3})) {
            return;
        }

        final Player player = (Player) sender;

        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

        // /pa bet [name] [amount]

        if (economy != null && this.betEnabled) {
            if (ap.getArenaTeam() != null) {
                debug(ap, "[Vault] tried to bet while playing the arena");
                this.arena.msg(player, MSG.MODULE_VAULT_BETNOTYOURS);
                return;
            }

            if (!economy.hasAccount(player)) {
                debug(ap, "[Vault] Account not found");
                this.arena.msg(sender, MSG.MODULE_VAULT_NOACCOUNT);
                return;
            }

            if (!this.arena.isFightInProgress()) {
                debug(ap, "[Vault] tried to bet before start");
                this.arena.msg(player, MSG.ERROR_VAULT_BEFOREBETTIME);
                return;
            }

            final int maxTime = this.arena.getConfig().getInt(CFG.MODULES_VAULT_BET_TIME);
            if (maxTime > 0 && this.arena.getPlayedSeconds() > maxTime) {
                debug(ap, "[Vault] tried to bet after max time");
                this.arena.msg(player, MSG.ERROR_VAULT_BETTIMEOVER);
                return;
            }

            String betObject;

            if(this.arena.isFreeForAll()) {
                Optional<String> matchedPlayer = this.arena.getFighters().stream()
                        .filter(p -> args[1].trim().equalsIgnoreCase(p.getName()))
                        .findAny()
                        .map(ArenaPlayer::getName);

                if(matchedPlayer.isEmpty()) {
                    this.arena.msg(player, MSG.MODULE_VAULT_BETONLYPLAYERS);
                    return;
                }

                betObject = matchedPlayer.get();
            } else {
                Optional<String> matchedTeam = this.arena.getNotEmptyTeams().stream()
                        .filter(p -> args[1].trim().equalsIgnoreCase(p.getName()))
                        .findAny()
                        .map(ArenaTeam::getName);

                if(matchedTeam.isEmpty()) {
                    this.arena.msg(player, MSG.MODULE_VAULT_BETONLYTEAMS);
                    return;
                }

                betObject = matchedTeam.get();
            }
            final double amount;

            try {
                amount = this.checkAndParseBetAmount(args[2], player);
            } catch (GameplayExceptionNotice e) {
                this.arena.msg(player, e.getMessage());
                return;
            }

            if(this.playerBetMap.containsKey(player.getUniqueId())) {
                Double amountToRefund = this.playerBetMap.get(player.getUniqueId()).getValue();
                economy.depositPlayer(player, amountToRefund);
                economy.withdrawPlayer(player, amount);
                this.arena.msg(player, MSG.MODULE_VAULT_BETCHANGED, economy.format(amount), betObject);
                this.playerBetMap.replace(player.getUniqueId(), new SimpleEntry<>(betObject, amount));
            } else {
                economy.withdrawPlayer(player, amount);
                this.arena.msg(player, MSG.MODULE_VAULT_BETPLACED, economy.format(amount), betObject);
                this.playerBetMap.put(player.getUniqueId(), new SimpleEntry<>(betObject, amount));
            }
        } else {
            this.arena.msg(sender, MSG.ERROR_COMMAND_UNKNOWN);
        }
    }

    @Override
    public void timedEnd(Set<String> winners) {
        if(this.arena.isFreeForAll()) {
            this.winnerNumber = winners.size();
        } else {
            this.winnerNumber = this.arena.getTeams().stream()
                    .filter(team -> winners.contains(team.getName()))
                    .mapToInt(team -> team.getTeamMembers().size())
                    .sum();
        }

        this.rewardGamblers();
    }

    @Override
    public boolean commitEnd(ArenaTeam winningTeam, ArenaPlayer maybeWinningPlayer) {
        this.endPlayerNumber = this.arena.getFighters().size();
        if(this.arena.isFreeForAll()) {
            this.winnerNumber = 1;
        } else {
            this.winnerNumber = winningTeam.getTeamMembers().size();
        }

        this.rewardGamblers();
        return false;
    }

    @Override
    public void parseStart() {
        this.activeListener = new VaultListener(this);
        Bukkit.getPluginManager().registerEvents(this.activeListener, PVPArena.getInstance());
    }

    /* This method is called only for winners after the end of the match */
    @Override
    public void giveRewards(final ArenaPlayer player) {
        if (player == null) {
            return;
        }

        Config cfg = this.arena.getConfig();
        final int minPlayTime = cfg.getInt(CFG.MODULES_VAULT_COND_MINPLAYTIME);

        if (minPlayTime > this.arena.getPlayedSeconds()) {
            debug(this.arena, "[Vault] no rewards, game too short!");
            return;
        }

        final int minPlayers = cfg.getInt(CFG.MODULES_VAULT_COND_MINPLAYERS);

        if (minPlayers > this.endPlayerNumber) {
            debug(this.arena, "[Vault] no rewards, not enough players!");
            return;
        }

        double rewardAmount = cfg.getDouble(CFG.MODULES_VAULT_REWARD_WIN);
        double potShare = 0;
        debug(player, "will receive Vault rewards. Plain reward: {}", rewardAmount);

        if (cfg.getBoolean(CFG.MODULES_VAULT_COND_WINFEEPOT)) {
            double winFactor = cfg.getDouble(CFG.MODULES_VAULT_REWARD_WINFACTOR);
            if(this.winnerNumber == 0) {
                potShare = cfg.getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE) * winFactor;
                PVPArena.getInstance().getLogger().warning(String.format("Impossible to know number of winner for goal %s. Please report this issue.", this.arena.getGoal().getName()));
            } else {
                potShare = (this.pot / this.winnerNumber) * winFactor;
                debug(player, "[Vault] Computed pot share: {}", potShare);
            }
        }


        this.depositRewardToPlayer(player.getPlayer(), rewardAmount + potShare);
    }

    @Override
    public void displayInfo(final CommandSender player) {
        Config cfg = this.arena.getConfig();
        player.sendMessage("Conditions settings:");
        player.sendMessage(String.format("entryfee: %s || winFeePot: %s || minPlayTime: %s || minPlayers: %s",
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE)),
                StringParser.colorVar(cfg.getBoolean(CFG.MODULES_VAULT_COND_WINFEEPOT)),
                StringParser.colorVar(cfg.getInt(CFG.MODULES_VAULT_COND_MINPLAYTIME)),
                StringParser.colorVar(cfg.getInt(CFG.MODULES_VAULT_COND_MINPLAYERS))));

        player.sendMessage("Reward settings:");
        player.sendMessage(String.format("win: %s || winFactor: %s || death: %s || kill: %s || score: %s",
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_REWARD_WIN)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_REWARD_WINFACTOR)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_REWARD_DEATH)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_REWARD_KILL)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_REWARD_SCORE))));

        player.sendMessage("Bet settings:");
        player.sendMessage(String.format("enabled: %s || time: %s || winFactor: %s || minAmount: %s || maxAmount: %s",
                StringParser.colorVar(cfg.getBoolean(CFG.MODULES_VAULT_BET_ENABLED)),
                StringParser.colorVar(cfg.getInt(CFG.MODULES_VAULT_BET_TIME)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_BET_WINFACTOR)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_BET_MINAMOUNT)),
                StringParser.colorVar(cfg.getDouble(CFG.MODULES_VAULT_BET_MAXAMOUNT))));
    }

    @Override
    public void onThisLoad() {
        if (economy == null && Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            final RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        final double entryfee = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE);
        if (entryfee > 0) {
            if (economy != null) {
                economy.withdrawPlayer(player, entryfee);
                this.arena.msg(player, MSG.MODULE_VAULT_JOINPAY, economy.format(entryfee));
                this.pot += entryfee;
            }
        }
        this.endPlayerNumber++;
    }

    @Override
    public void reset(final boolean force) {
        this.playerBetMap.clear();
        this.pot = 0;
        HandlerList.unregisterAll(this.activeListener);
        this.activeListener = null;
    }

    @Override
    public void resetPlayer(final Player player, final boolean soft, final boolean force) {
        if (player == null) {
            return;
        }

        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        if (ap == null || ap.getStatus() == null) {
            return;
        }

        if (ap.getStatus() == PlayerStatus.LOUNGE || ap.getStatus() == PlayerStatus.READY ||
                (asList(PlayerStatus.DEAD, PlayerStatus.FIGHT).contains(ap.getStatus()) && force)) {
            double entryFee = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_COND_ENTRYFEE);
            if (entryFee > 0) {
                this.arena.msg(player, MSG.MODULE_VAULT_REFUNDING, economy.format(entryFee));
                debug(ap, "[Vault] player has been refunded {}", economy.format(entryFee));
                this.depositToPlayer(player.getPlayer(), entryFee);
                this.pot -= entryFee;
            }
        }

        if (force) {
            debug("[Vault] Force reset detected, refunding gamblers");
            this.playerBetMap.forEach((uuid, betInfo) -> this.depositToPlayer(Bukkit.getOfflinePlayer(uuid), betInfo.getValue()));
            this.playerBetMap.clear();
        }
    }

    public void rewardPlayerForAction(Player player, CFG rewardCfgEntry) {
        debug("[Vault] Trying to reward player '{}' basing on entry '{}'", player, rewardCfgEntry.name());
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(String.format("[Vault] an unknown player triggered a reward in %s", this.arena.getName()));
            return;
        }
        Config config = this.arena.getConfig();
        double rewardAmount = config.getDouble(rewardCfgEntry);

        if(rewardAmount > 0) {
            debug(player, "[Vault] Player has earned {} ({})", economy.format(rewardAmount), rewardCfgEntry.name());
            economy.depositPlayer(player, rewardAmount);
            this.arena.msg(player, Language.parse(MSG.MODULE_VAULT_YOUEARNED, economy.format(rewardAmount)));
            String announceMsg = Language.parse(MSG.NOTICE_REWARDEDPLAYER, player.getName(), economy.format(rewardAmount));
            ArenaModuleManager.announce(this.arena, announceMsg, "PRIZE");
        }
    }

    private void depositRewardToPlayer(OfflinePlayer offlinePlayer, double amount) {
        if (economy != null) {
            BigDecimal total = BigDecimal.valueOf(amount);
            double roundedTotal = total.setScale(2, RoundingMode.HALF_UP).doubleValue();
            EconomyResponse res = economy.depositPlayer(offlinePlayer, roundedTotal);
            if (res.transactionSuccess()) {
                debug("[Vault] {} has received Vault reward: {}", offlinePlayer.getName(), roundedTotal);
                if(offlinePlayer.isOnline()) {
                    this.arena.msg(offlinePlayer.getPlayer(), Language.parse(MSG.MODULE_VAULT_YOUEARNED, economy.format(roundedTotal)));
                }
            } else {
                debug("[Vault] {} FAILED to received Vault reward: {}", offlinePlayer.getName(), roundedTotal);
            }
        }
    }

    private void depositToPlayer(OfflinePlayer offlinePlayer, double amount) {
        if (economy != null) {
            EconomyResponse res = economy.depositPlayer(offlinePlayer, amount);
            if (res.transactionSuccess()) {
                trace("[Vault] ${} have been deposited to {}'s account", amount, offlinePlayer.getName());
            } else {
                debug("[Vault] FAILED to deposit ${} to {}'s account", amount, offlinePlayer.getName());
            }
        }
    }

    private void rewardGamblers() {
        double betFactor = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BET_WINFACTOR);
        Set<String> winners = this.arena.getWinners();
        Map<UUID, Double> winnerBetMap = this.playerBetMap.entrySet().stream()
                .filter(entry -> winners.contains(entry.getValue().getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()));

        double totalGamblersBetAmount = this.playerBetMap.values().stream()
                .mapToDouble(SimpleEntry::getValue)
                .sum();
        double totalWinnersBetAmount = winnerBetMap.values().stream().reduce(0d, Double::sum);

        winnerBetMap.forEach((gamblerUUID, betAmount) -> {
                OfflinePlayer gambler = Bukkit.getOfflinePlayer(gamblerUUID);
                double betReward = (betAmount / totalWinnersBetAmount) * totalGamblersBetAmount * betFactor;
                this.depositRewardToPlayer(gambler, betReward);
        });

        this.playerBetMap.clear();
    }

    private double checkAndParseBetAmount(String amountArg, Player gambler) throws GameplayExceptionNotice {
        final double amount;

        try {
            amount = Double.parseDouble(amountArg);
        } catch (Exception e) {
            throw new GameplayExceptionNotice(Language.parse(MSG.MODULE_VAULT_INVALIDAMOUNT, amountArg));
        }

        if (!economy.has(gambler, amount)) {
            // no money, no entry!
            throw new GameplayExceptionNotice(Language.parse(MSG.MODULE_VAULT_NOTENOUGH, economy.format(amount)));
        }

        double maxAmount = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BET_MAXAMOUNT);
        double minAmount = this.arena.getConfig().getDouble(CFG.MODULES_VAULT_BET_MINAMOUNT);

        if (amount < minAmount || (maxAmount > 0 && amount > maxAmount)) {
            // wrong amount!
            throw new GameplayExceptionNotice(Language.parse(MSG.MODULE_VAULT_WRONGAMOUNT, economy.format(minAmount), economy.format(maxAmount)));
        }

        return amount;
    }
}
