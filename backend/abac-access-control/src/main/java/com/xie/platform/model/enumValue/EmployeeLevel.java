package com.xie.platform.model.enumValue;

public enum EmployeeLevel {

    P1(1), P2(2), P3(3), P4(4), P5(5),
    P6(6), P7(7), P8(8),
    VP(9), DIRECTOR(10);

    private final int rank;

    EmployeeLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean higherThan(EmployeeLevel other) {
        return this.rank > other.rank;
    }

    /**
     * 根据 rank 值反向查找枚举
     */
    public static EmployeeLevel fromRank(int rank) {
        for (EmployeeLevel level : values()) {
            if (level.rank == rank) {
                return level;
            }
        }
        throw new IllegalArgumentException("非法的员工职级: " + rank);
    }
}
