import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import solver.SudokuSolverFactory;
import solver.SudokuStepFinder;
import sudoku.Candidate;
import sudoku.Chain;
import sudoku.SolutionStep;
import sudoku.Sudoku2;

public class Wings {
    private static SudokuStepFinder finder;

    private static String cell(int i) {
        return "r" + (i / 9 + 1) + "c" + (i % 9 + 1);
    }

    private static boolean sees(int a, int b) {
        return a != b && Sudoku2.buddies[a].contains(b);
    }

    private static boolean has(Sudoku2 s, int i, int d) {
        return s.getValue(i) == 0 && s.isCandidate(i, d);
    }

    private static List<Integer> partners(Sudoku2 s, int from, int digit) {
        List<Integer> out = new ArrayList<>();
        for (int u = 0; u < 3; u++) {
            int other = -1;
            int n = 0;
            for (int c : Sudoku2.ALL_UNITS[Sudoku2.CONSTRAINTS[from][u]]) {
                if (has(s, c, digit)) {
                    n++;
                    if (c != from) other = c;
                }
            }
            if (n == 2 && !out.contains(other)) out.add(other);
        }
        return out;
    }

    private static class Wing {
        final int[] cells;
        final List<int[]> del;
        final String head;

        Wing(int[] cells, List<int[]> del, String head) {
            this.cells = cells;
            this.del = del;
            this.head = head;
        }
    }

    private static List<Wing> findM(Sudoku2 s) {
        List<Wing> res = new ArrayList<>();
        for (int a = 0; a < 81; a++) {
            if (s.getValue(a) != 0 || s.getAnzCandidates(a) != 2) continue;
            int[] ab = s.getAllCandidates(a);
            for (int k = 0; k < 2; k++) {
                int x = ab[k];
                int y = ab[1 - k];
                for (int b = 0; b < 81; b++) {
                    if (!sees(a, b) || !has(s, b, y)) continue;
                    for (int c : partners(s, b, y)) {
                        if (c == a || !has(s, c, x)) continue;
                        for (int d : partners(s, c, x)) {
                            if (d == a || d == b) continue;
                            List<int[]> del = new ArrayList<>();
                            for (int e = 0; e < 81; e++) {
                                if (e != a && e != d && has(s, e, x) && sees(e, a) && sees(e, d)) del.add(new int[] {e, x});
                            }
                            if (!del.isEmpty()) res.add(new Wing(new int[] {a, b, c, d}, del,
                                    "M-Wing: " + x + "/" + y + " in " + cell(a) + ", " + y + " " + cell(b) + "=" + cell(c)
                                    + ", " + x + " " + cell(c) + "=" + cell(d)));
                        }
                    }
                }
            }
        }
        return res;
    }

    private static List<Wing> findL(Sudoku2 s) {
        List<Wing> res = new ArrayList<>();
        for (int a = 0; a < 81; a++) {
            for (int x = 1; x <= 9; x++) {
                if (!has(s, a, x)) continue;
                for (int b : partners(s, a, x)) {
                    for (int y = 1; y <= 9; y++) {
                        if (y == x || !has(s, b, y)) continue;
                        for (int c : partners(s, b, y)) {
                            if (c == a) continue;
                            for (int z = 1; z <= 9; z++) {
                                if (z == x || z == y || !has(s, c, z)) continue;
                                for (int d : partners(s, c, z)) {
                                    if (d == a || d == b || !sees(a, d)) continue;
                                    List<int[]> del = new ArrayList<>();
                                    if (has(s, a, z)) del.add(new int[] {a, z});
                                    if (has(s, d, x)) del.add(new int[] {d, x});
                                    if (!del.isEmpty()) res.add(new Wing(new int[] {a, b, c, d}, del,
                                            "L-Wing: " + x + " " + cell(a) + "=" + cell(b) + ", " + y + " " + cell(b) + "=" + cell(c)
                                            + ", " + z + " " + cell(c) + "=" + cell(d)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private static List<Wing> findS(Sudoku2 s) {
        List<Wing> res = new ArrayList<>();
        for (int c = 0; c < 81; c++) {
            if (s.getValue(c) != 0 || s.getAnzCandidates(c) != 2) continue;
            int[] xy = s.getAllCandidates(c);
            for (int k = 0; k < 2; k++) {
                int x = xy[k];
                int y = xy[1 - k];
                for (int b = 0; b < 81; b++) {
                    if (!sees(b, c) || !has(s, b, x)) continue;
                    for (int a : partners(s, b, x)) {
                        if (a == c) continue;
                        for (int d = 0; d < 81; d++) {
                            if (d == a || d == b || !sees(c, d) || !has(s, d, y)) continue;
                            for (int e : partners(s, d, y)) {
                                if (e == a || e == b || e == c || !sees(a, e)) continue;
                                List<int[]> del = new ArrayList<>();
                                if (has(s, a, y)) del.add(new int[] {a, y});
                                if (has(s, e, x)) del.add(new int[] {e, x});
                                if (!del.isEmpty()) res.add(new Wing(new int[] {a, b, c, d, e}, del,
                                        "S-Wing: " + x + " " + cell(a) + "=" + cell(b) + ", " + x + "/" + y + " in " + cell(c)
                                        + ", " + y + " " + cell(d) + "=" + cell(e)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private static boolean matches(SolutionStep step, Wing w) {
        Set<Integer> del = new HashSet<>();
        for (Candidate cand : step.getCandidatesToDelete()) {
            del.add(cand.getIndex() * 10 + cand.getValue());
        }
        Set<Integer> want = new HashSet<>();
        for (int c : w.cells) want.add(c);
        Set<Integer> extra = new HashSet<>();
        for (int[] e : w.del) {
            if (!del.contains(e[0] * 10 + e[1])) return false;
            if (!want.contains(e[0])) extra.add(e[0]);
        }
        for (Chain ch : step.getChains()) {
            Set<Integer> got = new HashSet<>();
            int[] nodes = ch.getChain();
            for (int i = ch.getStart(); i <= ch.getEnd(); i++) {
                got.add(Chain.getSCellIndex(nodes[i]));
            }
            got.removeAll(extra);
            if (got.equals(want)) return true;
        }
        return false;
    }

    private static String describe(Wing w) {
        StringBuilder e = new StringBuilder();
        for (int[] d : w.del) {
            if (e.length() > 0) e.append(", ");
            e.append(cell(d[0])).append("<>").append(d[1]);
        }
        return w.head + " => " + e;
    }

    public static String find(Sudoku2 s, char kind) {
        List<Wing> all = kind == 'M' ? findM(s) : kind == 'L' ? findL(s) : findS(s);
        if (all.isEmpty()) return null;
        if (finder == null) {
            finder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
        }
        List<SolutionStep> steps = new ArrayList<>();
        steps.addAll(finder.getAllChains(s));
        steps.addAll(finder.getAllNiceLoops(s));
        for (Wing w : all) {
            for (SolutionStep step : steps) {
                if (matches(step, w)) return describe(w) + " | HoDoKu: " + step.toString(2);
            }
        }
        return null;
    }
}
