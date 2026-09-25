package com.fongmi.android.tv.player.autoskip;

import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class LocalAutoSkipCoordinator {

    private String displayedProposal = "";

    public long effectiveOpening(History history, PlayerManager player, String historyKey) {
        if (history == null || player == null) return 0;
        if (history.getOpening() > 0) return history.getOpening();
        LocalAutoSkipStore.Entry entry = LocalAutoSkipStore.get(mediaId(historyKey, player, history));
        return entry.manualOpening ? 0 : entry.openingMs;
    }

    public long effectiveEnding(History history, PlayerManager player, String historyKey) {
        if (history == null || player == null) return 0;
        if (history.getEnding() > 0) return history.getEnding();
        LocalAutoSkipStore.Entry entry = LocalAutoSkipStore.get(mediaId(historyKey, player, history));
        return entry.manualEnding ? 0 : entry.endingMs;
    }

    public void inspect(FragmentActivity activity, History history, PlayerManager player, String historyKey) {
        if (activity == null || activity.isFinishing() || history == null || player == null || player.isEmpty()) return;
        long durationMs = duration(history, player);
        String mediaId = mediaId(historyKey, player, history);
        if (TextUtils.isEmpty(mediaId)) return;
        LocalAutoSkipStore.Entry saved = LocalAutoSkipStore.get(mediaId);
        boolean openingAllowed = history.getOpening() <= 0 && !saved.manualOpening && saved.openingMs <= 0;
        boolean endingAllowed = history.getEnding() <= 0 && !saved.manualEnding && saved.endingMs <= 0;
        AutoSkipChapterPolicy.Proposal proposal = AutoSkipChapterPolicy.detect(
                player.getCurrentMediaEditions(), durationMs, openingAllowed, endingAllowed);
        if (proposal.isEmpty() || TextUtils.equals(saved.rejectedFingerprint, proposal.getFingerprint())) return;
        String proposalId = mediaId + ':' + proposal.getFingerprint();
        if (TextUtils.equals(displayedProposal, proposalId)) return;
        displayedProposal = proposalId;

        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_WebHTV_LightDialog)
                .setTitle(R.string.player_auto_skip_candidate_title)
                .setMessage(activity.getString(R.string.player_auto_skip_candidate_message, describe(activity, proposal)))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    LocalAutoSkipStore.accept(mediaId, proposal);
                    long openingMs = proposal.getOpeningMs();
                    if (openingMs > player.getPosition()) player.seekTo(openingMs);
                    Notify.show(R.string.player_auto_skip_candidate_applied);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> LocalAutoSkipStore.reject(mediaId, proposal.getFingerprint()))
                .show();
    }

    public void markManualOpening(History history, PlayerManager player, String historyKey) {
        LocalAutoSkipStore.markManual(mediaId(historyKey, player, history), true);
    }

    public void markManualEnding(History history, PlayerManager player, String historyKey) {
        LocalAutoSkipStore.markManual(mediaId(historyKey, player, history), false);
    }

    private static String describe(FragmentActivity activity, AutoSkipChapterPolicy.Proposal proposal) {
        StringBuilder text = new StringBuilder();
        if (proposal.getOpeningMs() > 0) {
            text.append(activity.getString(R.string.play_op)).append(' ')
                    .append(Util.timeMs(proposal.getOpeningMs()));
        }
        if (proposal.getEndingMs() > 0) {
            if (text.length() > 0) text.append(" / ");
            text.append(activity.getString(R.string.play_ed)).append(' ')
                    .append(Util.timeMs(proposal.getEndingMs()));
        }
        return text.toString();
    }

    private static String mediaId(String historyKey, PlayerManager player, History history) {
        if (player == null) return "";
        String url = player.getUrl();
        if (url == null) url = "";
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        int cut = query < 0 ? fragment : fragment < 0 ? query : Math.min(query, fragment);
        if (cut >= 0) url = url.substring(0, cut);
        long durationBucket = Math.round(duration(history, player) / 5000d) * 5L;
        return AutoSkipChapterPolicy.digest((historyKey == null ? "" : historyKey) + '|' + url + '|' + durationBucket);
    }

    private static long duration(History history, PlayerManager player) {
        return Math.max(player == null ? 0 : player.getDuration(), history == null ? 0 : history.getDuration());
    }
}
