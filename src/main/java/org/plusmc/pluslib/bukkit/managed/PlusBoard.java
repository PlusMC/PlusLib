package org.plusmc.pluslib.bukkit.managed;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.plusmc.pluslib.bukkit.handlers.VariableHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A scoreboard that is used to display information to players
 */
public abstract class PlusBoard implements Tickable {
    private final Scoreboard scoreboard;
    private final List<Score> scores;
    private Objective objective;

    protected PlusBoard(String title) {
        scores = new ArrayList<>();
        if (Bukkit.getScoreboardManager() == null) {
            throw new IllegalStateException("Scoreboard manager is null");
        }
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(title, "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    protected PlusBoard(String title, Scoreboard scoreboard) {
        scores = new ArrayList<>();
        if (Bukkit.getScoreboardManager() == null) {
            throw new IllegalStateException("Scoreboard manager is null");
        }
        this.scoreboard = scoreboard;
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);
        objective = scoreboard.getObjective(title);
        if (objective != null)
            objective.unregister();

        objective = scoreboard.registerNewObjective(title, "dummy", title);

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }


    //added a bunch of comments cause this shit is pretty confusing
    //im going to cry if this doesn't work
    @Override
    public void tick(long tick) {
        List<Score> newScores = new ArrayList<>();
        List<Score> scoresToRemove = new ArrayList<>();
        List<String> entries = new ArrayList<>(getEntries(tick));
        resolveList(entries);
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);

            //if the score isn't already in the scoreboard, add it
            if (i >= scores.size()) {
                Score score = objective.getScore(entry);
                newScores.add(score);
                continue;
            }

            //if the score is already in the scoreboard, re-add it
            Score score = scores.get(i);
            if (!score.getEntry().equals(entry)) {
                scoresToRemove.add(score);
                Score newScore = objective.getScore(entry);
                newScores.add(newScore);
            } else newScores.add(score);
        }
        scores.clear();
        scores.addAll(newScores);
        clearScores(scoresToRemove);
        addScores(scores);
    }

    /**
     * Gets the entries to be displayed on the scoreboard.
     *
     * @param tick current tick
     * @return the entries to be displayed on the scoreboard
     */
    public abstract List<String> getEntries(long tick);

    private void resolveList(List<String> entries) {
        Map<String, Integer> repeats = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            if (useVariables())
                entry = VariableHandler.formatString(entry);

            if (repeats.containsKey(entry))
                entry = entry + "§r".repeat(repeats.get(entry));

            repeats.put(entry, repeats.getOrDefault(entry, 0) + 1);
            entries.set(i, entry);
        }
    }

    private void clearScores(List<Score> scores) {
        for (Score score : scores) {
            scoreboard.resetScores(score.getEntry());
        }
    }

    private void addScores(List<Score> scores) {
        int i = scores.size() - 1;
        for (Score score : scores) {
            score.setScore(i);
            i--;
        }
    }

    public boolean useVariables() {
        return false;
    }

    /**
     * Sets the title of the scoreboard
     *
     * @param title the title of the scoreboard
     */
    public void setTitle(String title) {
        objective.setDisplayName(title);
    }

    /**
     * Gets the scoreboard
     *
     * @return the scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

}
