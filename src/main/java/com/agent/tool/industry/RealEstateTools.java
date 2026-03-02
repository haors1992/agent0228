package com.agent.tool.industry;

import com.agent.tool.annotation.Tool;
import com.agent.tool.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 房产行业工具集
 * 提供房产估价、政策查询、客户管理等功能
 */
@Slf4j
@Component
public class RealEstateTools {

    /**
     * 房产估价工具
     * 根据位置、户型、年龄估算房价
     */
    @Tool(name = "housing_estimate", description = "根据房产信息（地点、户型、年龄）估算合理房价。输入格式：'地址,户型,年龄' 如 '朝阳区建国路,2房2厅,5年'")
    public ToolResult estimateHousingPrice(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 3) {
                return ToolResult.failure("housing_estimate", "格式错误，请提供：地址,户型,年龄");
            }

            String location = parts[0].trim();
            String housingType = parts[1].trim();
            int age = Integer.parseInt(parts[2].trim().replaceAll("[年]", ""));

            // 估价逻辑
            double basePrice = estimateBasePrice(location);
            double typeMultiplier = getTypeMultiplier(housingType);
            double ageDiscount = 1.0 - (age * 0.02); // 每年贬值2%

            double estimatedPrice = basePrice * typeMultiplier * ageDiscount;

            String result = String.format(
                    "📍 %s 估价结果\n" +
                            "户型：%s | 楼龄：%d年\n" +
                            "估价：%.0f 万元\n" +
                            "（基价：%.0f万 × 户型系数：%.2f × 年龄折扣：%.2f）\n\n" +
                            "💡 建议：该价格处于市场中等水平，具有竞争力",
                    location, housingType, age, estimatedPrice, basePrice, typeMultiplier, ageDiscount);

            return ToolResult.success("housing_estimate", result);

        } catch (NumberFormatException e) {
            return ToolResult.failure("housing_estimate", "年龄必须是数字");
        } catch (Exception e) {
            return ToolResult.failure("housing_estimate", "估价失败：" + e.getMessage());
        }
    }

    /**
     * 政策查询工具
     */
    @Tool(name = "policy_query", description = "查询房产相关政策（限购、限售、税费等）。输入为城市名，如：'北京' 或 '上海'")
    public ToolResult queryPolicy(String city) {
        try {
            String cityName = city.trim();
            if ("北京".equals(cityName)) {
                return ToolResult.success("policy_query",
                        "🏛️ 北京房产政策速查\n\n" +
                                "【限购政策】\n" +
                                "✓ 本市户口：可购2套房产\n" +
                                "✓ 外地户口：仅限购1套\n" +
                                "✓ 条件：需4年以上社保或纳税\n\n" +
                                "【限售政策】\n" +
                                "✓ 新房：全市2年限售\n" +
                                "✓ 二手房：部分区域2年限售\n\n" +
                                "【税费标准】\n" +
                                "✓ 契税：首套90㎡以下1%，90-144㎡1.5%，144㎡以上3%\n" +
                                "✓ 增值税：满2年免税，不满2年按5%征收\n" +
                                "✓ 个人所得税：满5年免税（唯一住房）\n" +
                                "✓ 中介费：2-3%（可议价）");
            } else if ("上海".equals(cityName)) {
                return ToolResult.success("policy_query",
                        "🏛️ 上海房产政策速查\n\n" +
                                "【限购政策】\n" +
                                "✓ 本市户口：可购2套房产\n" +
                                "✓ 外地户口：仅限购1套\n" +
                                "✓ 条件：需5年社保或纳税\n\n" +
                                "【税费标准】\n" +
                                "✓ 契税：1.5%-3%（按房价分档）\n" +
                                "✓ 增值税：满2年免税\n" +
                                "✓ 个人所得税：满5年免税（唯一住房）\n" +
                                "✓ 中介费：2%（固定）");
            } else if ("深圳".equals(cityName)) {
                return ToolResult.success("policy_query",
                        "🏛️ 深圳房产政策速查\n\n" +
                                "【限购限售】\n" +
                                "✓ 本市户口：无限制\n" +
                                "✓ 外地户口：仅限购1套\n" +
                                "✓ 新房：2年限售，二手房：3年限售\n\n" +
                                "【税费标准】\n" +
                                "✓ 契税：1%-3%\n" +
                                "✓ 增值税：满2年免税\n" +
                                "✓ 个人所得税：满5年免税");
            } else {
                return ToolResult.failure("policy_query",
                        "暂未收录 " + cityName + " 的政策。已支持：北京、上海、深圳");
            }
        } catch (Exception e) {
            return ToolResult.failure("policy_query", "查询失败：" + e.getMessage());
        }
    }

    /**
     * 客户匹配工具
     */
    @Tool(name = "client_matching", description = "根据客户需求推荐房产。输入格式：'预算(万),户型,地点' 如'500,2房2厅,朝阳区'")
    public ToolResult matchClients(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 3) {
                return ToolResult.failure("client_matching", "格式错误，请提供：预算,户型,地点");
            }

            double budget = Double.parseDouble(parts[0].trim());
            String type = parts[1].trim();
            String location = parts[2].trim();

            String recommendation = String.format(
                    "🏠 根据您的需求进行房源推荐\n\n" +
                            "【匹配条件】\n" +
                            "预算：%.0f万 | 户型：%s | 地点：%s\n\n" +
                            "【推荐房源】\n\n" +
                            "1️⃣ %s小区\n" +
                            "   价格：%.0f万 | 户型：%s | 楼层：7楼\n" +
                            "   优势：地铁附近，楼层好，南向采光\n" +
                            "   备注：新装修，可立即入住\n\n" +
                            "2️⃣ %s公馆\n" +
                            "   价格：%.0f万 | 户型：%s | 楼层：18楼\n" +
                            "   优势：学区房，视野开阔，物业好\n" +
                            "   备注：适合有小孩的家庭\n\n" +
                            "3️⃣ %s社区\n" +
                            "   价格：%.0f万 | 户型：%s | 楼层：中层\n" +
                            "   优势：投资潜力大，升值空间\n" +
                            "   备注：等待城市规划政策利好\n\n" +
                            "📞 建议您先实地看房，对比优劣",
                    budget, type, location,
                    location + "A", budget * 0.95, type,
                    location + "B", budget * 1.0, type,
                    location + "C", budget * 1.05, type);

            return ToolResult.success("client_matching", recommendation);

        } catch (NumberFormatException e) {
            return ToolResult.failure("client_matching", "预算必须是数字");
        } catch (Exception e) {
            return ToolResult.failure("client_matching", "匹配失败：" + e.getMessage());
        }
    }

    /**
     * 户型评估工具
     */
    @Tool(name = "housing_evaluation", description = "评估户型质量。输入格式：'面积(㎡),朝向,楼层,配套' 如'120,南北通透,7层,学区房'")
    public ToolResult evaluateHousing(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length < 2) {
                return ToolResult.failure("housing_evaluation", "请至少提供面积和朝向");
            }

            String evaluation = "🏠 户型评估报告\n\n";
            evaluation += "【关键指标评分】\n";
            evaluation += "✅ 朝向评分：9/10 - " + parts[1].trim() + " 的朝向是最优选择\n";
            if (parts.length > 2) {
                evaluation += "✅ 楼层评分：8/10 - " + parts[2].trim() + " 楼采光好，噪音少\n";
            }
            if (parts.length > 3) {
                evaluation += "✅ 配套评分：9/10 - " + parts[3].trim() + " 增值潜力\n";
            }

            evaluation += "\n【综合建议】\n";
            evaluation += "这套户型综合评分：8.5/10\n";
            evaluation += "✓ 适合：自住和投资都较为合适\n";
            evaluation += "✓ 建议：这个价位的房源中上等水平\n";
            evaluation += "✓ 决策：可以考虑进一步看房和谈价";

            return ToolResult.success("housing_evaluation", evaluation);

        } catch (Exception e) {
            return ToolResult.failure("housing_evaluation", "评估失败：" + e.getMessage());
        }
    }

    /**
     * 交易费用计算工具
     */
    @Tool(name = "transaction_fee", description = "计算房产交易的各项费用。输入格式：'房价(万),城市,户型面积' 如'500,北京,120'")
    public ToolResult calculateTransactionFee(String input) {
        try {
            String[] parts = input.split(",");
            if (parts.length != 3) {
                return ToolResult.failure("transaction_fee", "格式错误");
            }

            double price = Double.parseDouble(parts[0].trim());
            String city = parts[1].trim();
            double area = Double.parseDouble(parts[2].trim());

            // 按首套房计算
            double contractTax = calculateContractTax(city, price, area);
            double agencyFee = price * 0.02; // 中介费2%
            double appraisalFee = price * 0.005; // 评估费0.5%
            double registrationFee = 80; // 登记费固定

            double total = contractTax + agencyFee + appraisalFee + registrationFee;

            String result = String.format(
                    "💰 房产交易费用明细\n\n" +
                            "购入价格：%.0f 万元\n\n" +
                            "【各项费用】\n" +
                            "✓ 契税：%.1f 万元 (%s规定)\n" +
                            "✓ 中介费：%.1f 万元 (2%)\n" +
                            "✓ 评估费：%.1f 万元 (0.5%)\n" +
                            "✓ 登记费：%.2f 万元 (固定)\n" +
                            "✓ 其他税费：%.1f 万元 (待定)\n\n" +
                            "【总计】%.1f 万元\n\n" +
                            "💡 建议：与卖方协商，由卖方承担部分费用",
                    price, contractTax, city, agencyFee, appraisalFee, registrationFee / 10000,
                    0.5, total);

            return ToolResult.success("transaction_fee", result);

        } catch (Exception e) {
            return ToolResult.failure("transaction_fee", "计算失败：" + e.getMessage());
        }
    }

    // ===== 辅助方法 =====

    private double estimateBasePrice(String location) {
        if ("朝阳区建国路".equals(location))
            return 600.0;
        if ("浦东世纪大道".equals(location))
            return 650.0;
        if ("海淀中关村".equals(location))
            return 550.0;
        if ("朝阳区".equals(location))
            return 520.0;
        if ("浦东".equals(location))
            return 580.0;
        if ("海淀".equals(location))
            return 500.0;
        return 400.0;
    }

    private double getTypeMultiplier(String type) {
        if ("1房1厅".equals(type) || "1室1厅".equals(type))
            return 0.8;
        if ("2房2厅".equals(type) || "2室2厅".equals(type))
            return 1.0;
        if ("3房2厅".equals(type) || "3室2厅".equals(type))
            return 1.3;
        if ("3房3厅".equals(type) || "3室3厅".equals(type))
            return 1.4;
        if ("4房3厅".equals(type) || "4室3厅".equals(type))
            return 1.6;
        return 1.0;
    }

    private double calculateContractTax(String city, double price, double area) {
        // 按首套房计算
        if (area < 90) {
            return price * 0.01; // 1%
        } else if (area < 144) {
            return price * 0.015; // 1.5%
        } else {
            return price * 0.03; // 3%
        }
    }
}
