package com.notpatch.nOrder.model;

import com.notpatch.nOrder.Settings;
import lombok.Data;

@Data
public class ProgressBar {

    private int current;
    private int max;

    public ProgressBar(Order order) {
        this.current = order.getDelivered();
        this.max = order.getAmount();
    }

    public String render() {
        StringBuilder bar = new StringBuilder();
        int completeLength = (int) ((double) current / max * Settings.PROGRESS_BAR_LENGTH);
        for (int i = 0; i < Settings.PROGRESS_BAR_LENGTH; i++) {
            if (i < completeLength) {
                bar.append(Settings.PROGRESS_BAR_COMPLETE_COLOR).append(Settings.PROGRESS_BAR_COMPLETE_CHAR);
            } else {
                bar.append(Settings.PROGRESS_BAR_INCOMPLETE_COLOR).append(Settings.PROGRESS_BAR_INCOMPLETE_CHAR);
            }
        }
        return bar.toString();
    }

}
