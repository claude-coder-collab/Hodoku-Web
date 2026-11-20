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

public class MWing {
    private static SudokuStepFinder finder;

    private static String cell(int i) {
        return "r" + (i / 9 + 1) + "c" + (i % 9 + 1);
    }

    private static boolean sees(int a, int b) {
        return a != b && Sudoku2.buddies[a].contains(b);
    }

    private static void partner(Sudoku2 s, int from, int digit, int[] out) {
        for (int u = 0; u < 3; u++) {
            int other = -1;
            int n = 0;
            for (int c : Sudoku2.ALL_UNITS[Sudoku2.CONSTRAINTS[from][u]]) {
                if (s.getValue(c) == 0 && s.isCandidate(c, digit)) {
                    n++;
                    if (c != from) other = c;
                }
            }
            out[u] = n == 2 ? other : -1;
        }
    }

    private static List<int[]> findAll(Sudoku2 s) {
        List<int[]> res = new ArrayList<>();
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
                        if (c < 0 || c == a || !s.isCandidate(c, x)) continue;
                        partner(s, c, x, p2);
                        for (int d : p2) {
                            if (d < 0 || d == a || d == b) continue;
                            List<Integer> elim = new ArrayList<>();
                            for (int e = 0; e < 81; e++) {
                                if (e != a && e != d && s.getValue(e) == 0 && s.isCandidate(e, x) && sees(e, a) && sees(e, d)) {
                                    elim.add(e);
                                }
                            }
                            if (elim.isEmpty()) continue;
                            int[] m = new int[6 + elim.size()];
                            m[0] = a; m[1] = b; m[2] = c; m[3] = d; m[4] = x; m[5] = y;
                            for (int i = 0; i < elim.size(); i++) m[6 + i] = elim.get(i);
                            res.add(m);
                        }
                    }
                }
            }
        }
        return res;
    }

    private static boolean matches(SolutionStep step, int[] m) {
        Set<Integer> del = new HashSet<>();
        for (Candidate cand : step.getCandidatesToDelete()) {
            if (cand.getValue() == m[4]) del.add(cand.getIndex());
        }
        for (int i = 6; i < m.length; i++) {
            if (!del.contains(m[i])) return false;
        }
        Set<Integer> want = new HashSet<>();
        for (int i = 0; i < 4; i++) want.add(m[i]);
        for (Chain ch : step.getChains()) {
            Set<Integer> got = new HashSet<>();
            int[] nodes = ch.getChain();
            for (int i = ch.getStart(); i <= ch.getEnd(); i++) {
                got.add(Chain.getSCellIndex(nodes[i]));
            }
            if (got.equals(want)) return true;
        }
        return false;
    }

    public static String find(Sudoku2 s) {
        List<int[]> all = findAll(s);
        if (all.isEmpty()) return null;
        if (finder == null) {
            finder = SudokuSolverFactory.getDefaultSolverInstance().getStepFinder();
        }
        List<SolutionStep> steps = new ArrayList<>();
        steps.addAll(finder.getAllChains(s));
        steps.addAll(finder.getAllNiceLoops(s));
        for (int[] m : all) {
            for (SolutionStep step : steps) {
                if (!matches(step, m)) continue;
                StringBuilder e = new StringBuilder();
                for (int i = 6; i < m.length; i++) {
                    if (e.length() > 0) e.append(',');
                    e.append(cell(m[i]));
                }
                return "M-Wing: " + m[4] + "/" + m[5] + " in " + cell(m[0]) + ", " + m[5] + " " + cell(m[1]) + "=" + cell(m[2])
                        + ", " + m[4] + " " + cell(m[2]) + "=" + cell(m[3]) + " => " + e + "<>" + m[4]
                        + " | HoDoKu: " + step.toString(2);
            }
        }
        return null;
    }
}
