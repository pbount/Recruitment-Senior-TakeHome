package com.automwrite.assessment.service.transposition;

public interface ToneManagementService<T> {

    StylisticTone extractTone(T file) throws Exception;

    T applyTone(T file, StylisticTone tone) throws Exception;

}
