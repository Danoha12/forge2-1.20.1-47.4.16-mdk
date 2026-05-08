package com.trolmastercard.sexmod.tribe;

import com.trolmastercard.sexmod.entity.KoboldEntity;

public class Task {
    // Por ahora lo dejamos simple para que compile
    public boolean isAssignedTo(KoboldEntity kobold) { return false; }
    public boolean isDone(KoboldEntity kobold) { return true; }
    public void complete(KoboldEntity kobold) {}
    public void completeForKobold(KoboldEntity kobold) {}
}