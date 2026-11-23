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

    private static int[] wing(int a, int b, int c, int d, int x, int y, int z, List<int[]> del) {
        int[] w = new int[7 + 2 * del.size()];
        w[0] = a; w[1] = b; w[2] = c; w[3] = d; w[4] = x; w[5] = y; w[6] = z;
        for (int i = 0; i < del.size(); i++) {
            w[7 + 2 * i] = del.get(i)[0];
            w[8 + 2 * i] = del.get(i)[1];
        }
        return w;
    }

    private static List<int[]> findM(Sudoku2 s) {
        List<int[]> res = new ArrayList<>();
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
                            if (!del.isEmpty()) res.add(wing(a, b, c, d, x, y, x, del));
                        }
                    }
                }
            }
        }
        return res;
    }

    private static List<int[]> findL(Sudoku2 s) {
        List<int[]> res = new ArrayList<>();
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
                                    if (!del.isEmpty()) res.add(wing(a, b, c, d, x, y, z, del));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private static boolean matches(SolutionStep step, int[] w) {
        Set<Integer> del = new HashSet<>();
        for (Candidate cand : step.getCandidatesToDelete()) {
            del.add(cand.getIndex() * 10 + cand.getValue());
        }
        Set<Integer> want = new HashSet<>();
        for (int i = 0; i < 4; i++) want.add(w[i]);
        Set<Integer> extra = new HashSet<>();
        for (int i = 7; i < w.length; i += 2) {
            if (!del.contains(w[i] * 10 + w[i + 1])) return false;
            if (!want.contains(w[i])) extra.add(w[i]);
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

    private static String describe(boolean m, int[] w) {
        StringBuilder e = new StringBuilder();
        for (int i = 7; i < w.length; i += 2) {
            if (e.length() > 0) e.append(", ");
            e.append(cell(w[i])).append("<>").append(w[i + 1]);
        }
        String head = m
                ? "M-Wing: " + w[4] + "/" + w[5] + " in " + cell(w[0]) + ", " + w[5] + " " + cell(w[1]) + "=" + cell(w[2])
                : "L-Wing: " + w[4] + " " + cell(w[0]) + "=" + cell(w[1]) + ", " + w[5] + " " + cell(w[1]) + "=" + cell(w[2]);
        return head + ", " + w[6] + " " + cell(w[2]) + "=" + cell(w[3]) + " => " + e;
    }

    public static String find(Sudoku2 s, boolean m) {
        List<int[]> all = m ? findM(s) : findL(s);
        if (all.isEmpty()) return null;
        if (finder == null) {
            finder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
        }
        List<SolutionStep> steps = new ArrayList<>();
        steps.addAll(finder.getAllChains(s));
        steps.addAll(finder.getAllNiceLoops(s));
        for (int[] w : all) {
            for (SolutionStep step : steps) {
                if (matches(step, w)) return describe(m, w) + " | HoDoKu: " + step.toString(2);
            }
        }
        return null;
    }
}
