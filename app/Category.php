<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Category extends Model
{
    public function charities()
    {
        return $this->hasMany(Charity::class);
    }
}
