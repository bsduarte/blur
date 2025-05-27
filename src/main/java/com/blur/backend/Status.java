package com.blur.backend;

import com.google.gson.annotations.SerializedName;

public enum Status {
    @SerializedName("active")
    ACTIVE,
    @SerializedName("done")
    DONE;
}
