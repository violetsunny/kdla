package top.kdla.framework.common.help;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author kllp0648
 * @version $Id: PinYinUtils.java, v 0.1 2017年5月9日 上午11:36:00 kllp0648 Exp $
 */
public class PinYinHelp {

    private PinYinHelp(){};
    

    /**
     * 汉子转拼音
     * 
     * @param chinese
     * @return
     */
    public static List<String> CH2PY(String chinese){
        char[] chars = chinese.toCharArray();
        String[][] pinYinArray = new String[chars.length][];
        for (int i = 0; i < chars.length; i++) {
            pinYinArray[i] = PinyinHelper.convertToPinyinArray(chars[i], PinyinFormat.WITHOUT_TONE);
        }
        List<String> resultPinYin = new ArrayList<>();
        //一字多音，组合拼
        getPinYin(pinYinArray, 0, "", resultPinYin);
        return resultPinYin;
    }
    /**
     * 递归计算多音字的拼音组合
     *
     * @param pinYinArray
     * @param index
     * @param currentStr
     * @param resultPinYin
     */
    private static void getPinYin(String[][] pinYinArray, int index, String currentStr, List<String> resultPinYin) {
        //如果是最后一个数组
        if (pinYinArray.length - 1 == index) {
            //如果最后一个数组没有任何元素
            if (pinYinArray[index].length == 0) {
                resultPinYin.add(currentStr);
            } else {
                for (String pinYin : pinYinArray[index]) {
                    resultPinYin.add(currentStr + pinYin);
                }
            }
        } else {
            //如果当前数组元素为空，如输入了X
            if (pinYinArray[index].length == 0) {
                getPinYin(pinYinArray, index + 1, currentStr, resultPinYin);
            } else {
                for (int i = 0; i < pinYinArray[index].length; i++) {
                    getPinYin(pinYinArray, index + 1, currentStr + pinYinArray[index][i], resultPinYin);
                }
            }
        }
    }
}
