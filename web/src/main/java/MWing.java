import sudoku.Sudoku2;

public class MWing {
    private static String cell(int i) {
        return "r" + (i / 9 + 1) + "c" + (i % 9 + 1);
    }

    private static boolean sees(int a, int b) {
        return a != b && Sudoku2.buddies[a].contains(b);
    }

    private static void partner(Sudoku2 s, int from, int digit, int[] unitOut) {
        for (int u = 0; u < 3; u++) {
            int[] unit = Sudoku2.ALL_UNITS[Sudoku2.CONSTRAINTS[from][u]];
            int other = -1;
            int n = 0;
            for (int c : unit) {
                if (s.getValue(c) == 0 && s.isCandidate(c, digit)) {
                    n++;
                    if (c != from) other = c;
                }
            }
            if (n == 2) {
                unitOut[u] = other;
            } else {
                unitOut[u] = -1;
            }
        }
    }

    public static String find(Sudoku2 s) {
        int[] p1 = new int[3];
        int[] p2 = new int[3];
        for (int a = 0; a < 81; a++) {
            if (s.getValue(a) != 0 || s.getAnzCandidates(a) != 2) continue;
            int[] ab = s.getAllCandidates(a);
            for (int k = 0; k < 2; k++) {
                int x = ab[k];
                int y = ab[1 - k];
                for (int b = 0; b < 81; b++) {
                    if (!sees(a, b) || s.getValue(b) != 0 || !s.isCandidate(b, y)) continue;
                    partner(s, b, y, p1);
                    for (int c : p1) {
                        if (c < 0 || c == a || s.isCandidate(c, x) == false) continue;
                        partner(s, c, x, p2);
                        for (int d : p2) {
                            if (d < 0 || d == a || d == b) continue;
                            StringBuilder elim = new StringBuilder();
                            for (int e = 0; e < 81; e++) {
                                if (e == a || e == d || s.getValue(e) != 0 || !s.isCandidate(e, x)) continue;
                                if (sees(e, a) && sees(e, d)) {
                                    if (elim.length() > 0) elim.append(',');
                                    elim.append(cell(e));
                                }
                            }
                            if (elim.length() == 0) continue;
                            return "M-Wing: " + x + "/" + y + " in " + cell(a) + ", " + y + " " + cell(b) + "=" + cell(c)
                                    + ", " + x + " " + cell(c) + "=" + cell(d) + " => " + elim + "<>" + x;
                        }
                    }
                }
            }
        }
        return null;
    }
}
