package com.example.learnkeep;

public class TopicIconHelper {

    public static int getIconFromTags(String tags) {

        if (tags == null) return R.drawable.ic_default;

        tags = tags.toLowerCase();

        if (tags.contains("science"))
            return R.drawable.ic_science;
        else if (tags.contains("math")||tags.contains("mathematics"))
            return R.drawable.ic_math;
        else if (tags.contains("physics")||tags.contains("physic"))
            return R.drawable.ic_physics;
        else if (tags.contains("history"))
            return R.drawable.ic_history;
        else if (tags.contains("marathi"))
            return R.drawable.ic_marathi;
        else if (tags.contains("hindi"))
            return R.drawable.ic_hindi;
        else if (tags.contains("english"))
            return R.drawable.ic_english;
        else if (tags.contains("chemistry"))
            return R.drawable.ic_chemistry;
        else if (tags.contains("biology"))
            return R.drawable.ic_biology;
        else if (tags.contains("geography"))
            return R.drawable.ic_geography;
        else if (tags.contains("commerce"))
            return R.drawable.ic_commerce;
        else if (tags.contains("algebra")||tags.contains("geometry"))
            return R.drawable.ic_algebra;
        else if (tags.contains("accountancy"))
            return R.drawable.ic_accountancy;
        else if (tags.contains("coding")||tags.contains("programming")||tags.contains("c++")|| tags.contains("python")
                ||tags.contains("c")||tags.contains("java")||tags.contains("php")||tags.contains("computer"))
            return R.drawable.ic_code;
        else if (tags.contains("marwadi")||tags.contains("jitesh")||tags.contains("developer"))
            return R.drawable.ic_marwadi;

        return R.drawable.ic_default;
    }
}
