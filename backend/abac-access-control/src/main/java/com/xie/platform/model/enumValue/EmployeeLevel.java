package com.xie.platform.model.enumValue;

public enum  EmployeeLevel {

    // ? 是否保留为VARCHAR ，还是切换为 int类型
    
    P1(1), P2(2), P3(3), P4(4), P5(5),
    P6(6), P7(7), P8(8),
    VP(9), DIRECTOR(10);

    private final int rank;

    EmployeeLevel(int rank) {
        this.rank = rank;
    }

    public boolean higherThan(EmployeeLevel other) {
        return this.rank > other.rank;
    }
}
