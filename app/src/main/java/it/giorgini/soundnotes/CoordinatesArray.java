package it.giorgini.soundnotes;

import java.util.Arrays;

public class CoordinatesArray {
    private int[] baselineArray;
    private int[] interlinesArray;
    private boolean[] presenceArray;
    private int used;

    public CoordinatesArray(int size) {
        baselineArray = new int[size];
        interlinesArray = new int[size];
        presenceArray = new boolean[size];
        used = 0;
    }

    private void grow() {
        baselineArray = Arrays.copyOf(baselineArray, baselineArray.length * 2);
        interlinesArray = Arrays.copyOf(interlinesArray, interlinesArray.length * 2);
        presenceArray = Arrays.copyOf(presenceArray, presenceArray.length * 2);
    }

    private void checkIndexSanityAndAllocate(int i) {
        if (i < 0)
            throw new IndexOutOfBoundsException("Gli array hanno " + used + " elementi. i=" + i);
        else if (i >= used) {
            initializeCells(i);
        }
    }

    private void initializeCells(int lenght) {
        for (int j = used; j <= lenght; j++) {
            add(0, 0, false);
        }
    }

    public void add(int baseline, int interline, boolean presence) {
        if (used >= baselineArray.length)
            grow();
        baselineArray[used] = baseline;
        interlinesArray[used] = interline;
        presenceArray[used] = presence;
        used++;
    }

    public void set(int i, int baseline, int interline, boolean presence) {
        checkIndexSanityAndAllocate(i);
        baselineArray[i] = baseline;
        interlinesArray[i] = interline;
        presenceArray[i] = presence;
    }

    public int getBaseline(int i) {
        checkIndexSanityAndAllocate(i);
        return baselineArray[i];
    }

    public int getInterline(int i) {
        checkIndexSanityAndAllocate(i);
        return interlinesArray[i];
    }

    public int getLenght() {
        return used;
    }

    public boolean hasValues(int i) {
        checkIndexSanityAndAllocate(i);
        return presenceArray[i];
    }
}

//package it.giorgini.soundnotes;
//
//        import java.util.Arrays;
//
//public class CoordinatesArray {
//    private int[] baselineArray;
//    private int[] interlinesArray;
//    private boolean[] presenceArray;
//    private int used;
//
//    public CoordinatesArray(int size) {
//        baselineArray = new int[size];
//        interlinesArray = new int[size];
//        presenceArray = new boolean[size];
//        used = 0;
//    }
//
//    private void grow() {
//        baselineArray = Arrays.copyOf(baselineArray, baselineArray.length * 2);
//        interlinesArray = Arrays.copyOf(interlinesArray, interlinesArray.length * 2);
//        presenceArray = Arrays.copyOf(presenceArray, presenceArray.length * 2);
//    }
//
//    private void checkIndexSanityAndAllocate(int i) {
//        if (i < 0)
//            throw new IndexOutOfBoundsException("Gli array hanno " + used + " elementi. i=" + i);
//        else if (i >= used) {
//            initializeCells(i);
//        }
//    }
//
//    private void initializeCells(int lenght) {
//        for (int j = used; j <= lenght; j++) {
//            add(0, 0, false);
//        }
//    }
//
//    public void add(int baseline, int interline, boolean presence) {
//        if (used >= baselineArray.length)
//            grow();
//        baselineArray[used] = baseline;
//        interlinesArray[used] = interline;
//        presenceArray[used] = presence;
//        used++;
//    }
//
//    public void set(int i, int baseline, int interline, boolean presence) {
//        checkIndexSanityAndAllocate(i);
//        baselineArray[i] = baseline;
//        interlinesArray[i] = interline;
//        presenceArray[i] = presence;
//    }
//
//    public int getBaseline(int i) {
//        checkIndexSanityAndAllocate(i);
//        return baselineArray[i];
//    }
//
//    public int getInterline(int i) {
//        checkIndexSanityAndAllocate(i);
//        return interlinesArray[i];
//    }
//
//    public int getLenght() {
//        return used;
//    }
//
//    public boolean hasValues(int i) {
//        checkIndexSanityAndAllocate(i);
//        return presenceArray[i];
//    }
//}